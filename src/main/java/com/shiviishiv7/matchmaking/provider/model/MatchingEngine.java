package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.*;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.VALIDATION_ERROR;

/**
 * Central orchestrator for the 3-phase matching pipeline.
 *
 * Phase 1 — Hard filter   : delegate to CategoryScorer.fetchCandidateIds()
 * Phase 2 — Scoring       : delegate to CategoryScorer.score() for each candidate
 * Phase 3 — Post-process  : sort, deduplicate, paginate, persist MatchResult rows
 *
 * New categories are supported automatically by adding a new CategoryScorer bean.
 * The engine discovers all scorers at startup via Spring injection and routes
 * by MatchCategory using a registry map.
 */
@Service
@Slf4j
public class MatchingEngine {

    private final Map<MatchCategory, CategoryScorer> scorerRegistry = new HashMap<>();

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private BlockListRepository blockListRepository;

    /**
     * Spring injects all CategoryScorer beans here at startup.
     * Each scorer registers itself by its supported MatchCategory.
     */
    @Autowired
    public MatchingEngine(List<CategoryScorer> scorers) {
        for (CategoryScorer scorer : scorers) {
            scorerRegistry.put(scorer.supports(), scorer);
            log.info("Registered scorer for category: {}", scorer.supports());
        }
    }

    /**
     * Main entry point — called by MatchingProcessor.
     * Returns a paginated, scored, deduplicated list of match candidates.
     */
    public List<MatchCandidateVO> discover(MatchDiscoveryRequestVO request) throws MatchmakingException {
        String userId          = request.getCognitoSubA();
        MatchCategory category  = request.getMatchCategory();
        int page                = request.getPage();
        int pageSize            = request.getPageSize();

        log.info("Discovery request: userId={} category={} page={} pageSize={}",
                userId, category, page, pageSize);

        // ── Resolve scorer ────────────────────────────────────────────────────
        CategoryScorer scorer = scorerRegistry.get(category);
        if (scorer == null) {
            log.error("No scorer registered for category: {}", category);
            throw new MatchmakingException("Matching not supported for category: " + category, VALIDATION_ERROR);
        }

        // ── Build exclude list ────────────────────────────────────────────────
        // Exclude: already-seen candidates + blocked users
        Set<String> seenIds    = matchResultRepository.findSeenCandidateIds(userId, category);
        Set<String> blockedIds = blockListRepository.findAllBlockedUserIds(userId);

        List<String> excludeIds = new ArrayList<>();
        excludeIds.addAll(seenIds);
        excludeIds.addAll(blockedIds);

        log.trace("Excluding {} seen + {} blocked candidates for userId: {}",
                seenIds.size(), blockedIds.size(), userId);

        // ── Phase 1: Hard filter ──────────────────────────────────────────────
        List<String> candidateIds = scorer.fetchCandidateIds(userId, excludeIds);
        log.info("Phase 1 complete: {} candidates after hard filter for userId: {}", candidateIds.size(), userId);

        if (candidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        // ── Phase 2: Score each candidate ─────────────────────────────────────
        List<MatchCandidateVO> scored = new ArrayList<>();
        for (String candidateId : candidateIds) {
            try {
                MatchCandidateVO vo = scorer.score(userId, candidateId);
                scored.add(vo);
            } catch (Exception ex) {
                log.warn("Scoring failed for candidate {} — skipping. Error: {}", candidateId, ex.getMessage());
            }
        }
        log.info("Phase 2 complete: {} candidates scored for userId: {}", scored.size(), userId);

        // ── Phase 3: Sort, paginate, persist ──────────────────────────────────
        scored.sort(Comparator.comparingInt(MatchCandidateVO::getCompatibilityScore).reversed());

        int fromIndex = page * pageSize;
        if (fromIndex >= scored.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + pageSize, scored.size());
        List<MatchCandidateVO> page_results = scored.subList(fromIndex, toIndex);

        // Persist a MatchResult row for each candidate shown (status = PENDING)
        persistShownResults(userId, category, page_results);

        log.info("Discovery complete: returning {} results for userId={} category={}",
                page_results.size(), userId, category);

        return page_results;
    }

    /**
     * Called when a user acts on a match card (LIKED / SKIPPED).
     * If both users have LIKED each other → sets isMutual = true on both rows.
     */
    public void recordAction(String cognitoSubA, String cognitoSubB,
                              MatchCategory category, MatchStatus action) throws MatchmakingException {
        log.info("Recording action: userId={} candidateUserId={} category={} action={}",
                cognitoSubA, cognitoSubB, category, action);

        MatchResult result = matchResultRepository
                .findByCognitoSubAAndCognitoSubBAndMatchCategory(cognitoSubA, cognitoSubB, category)
                .orElseGet(() -> {
                    // Create on-the-fly if user acted without going through discovery (e.g. deep link)
                    MatchResult r = new MatchResult();
                    r.setCognitoSubA(cognitoSubA);
                    r.setCognitoSubB(cognitoSubB);
                    r.setMatchCategory(category);
                    r.setShownAt(LocalDateTime.now());
                    return r;
                });

        result.setStatus(action);
        result.setActedAt(LocalDateTime.now());
        matchResultRepository.save(result);

        // Check for mutual like
        if (action == MatchStatus.LIKED) {
            long mutualCount = matchResultRepository.countMutualLike(cognitoSubB, cognitoSubA, category);
            if (mutualCount > 0) {
                log.info("Mutual match detected! userId={} <-> candidateUserId={} category={}",
                        cognitoSubA, cognitoSubB, category);
                // Update both rows to isMutual = true
                result.setIsMutual(true);
                result.setStatus(MatchStatus.CONNECTED);
                matchResultRepository.save(result);

                matchResultRepository
                        .findByCognitoSubAAndCognitoSubBAndMatchCategory(cognitoSubB, cognitoSubA, category)
                        .ifPresent(mirror -> {
                            mirror.setIsMutual(true);
                            mirror.setStatus(MatchStatus.CONNECTED);
                            matchResultRepository.save(mirror);
                        });
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void persistShownResults(String userId, MatchCategory category,
                                      List<MatchCandidateVO> results) {
        for (MatchCandidateVO vo : results) {
            try {
                if (!matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(
                        userId, vo.getCognitoSubB(), category)) {
                    MatchResult mr = MatchResult.builder()
                            .cognitoSubA(userId)
                            .cognitoSubB(vo.getCognitoSubB())
                            .matchCategory(category)
                            .compatibilityScore(vo.getCompatibilityScore())
                            .scoreBreakdown(vo.getScoreBreakdown())
                            .status(MatchStatus.PENDING)
                            .isMutual(false)
                            .shownAt(LocalDateTime.now())
                            .build();
                    matchResultRepository.save(mr);
                }
            } catch (Exception ex) {
                log.warn("Could not persist match result for candidateUserId: {}. Error: {}",
                        vo.getCognitoSubB(), ex.getMessage());
            }
        }
    }
}
