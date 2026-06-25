package com.shiviishiv7.matchmaking.processor.matchingengine;



import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
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
public class MatchingEngineProcessor {

    private final Map<MatchCategory, CategoryScorer> scorerRegistry = new HashMap<>();

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private BlockListRepository blockListRepository;

    private static final int SCHEDULE_AHEAD_HOURS = 3;

    /**
     * Spring injects all CategoryScorer beans here at startup.
     * Each scorer registers itself by its supported MatchCategory.
     */
    @Autowired
    public MatchingEngineProcessor(List<CategoryScorer> scorers) {
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
        Set<String> seenIds = matchResultRepository.findSeenCandidateIds(userId, category);

        // BlockList stores integer IDs; parse userId if numeric, skip block lookup if it's a UUID-style cognitoSub
        Set<String> excludeIds = new HashSet<>(seenIds);
        try {
            Integer userIdInt = Integer.valueOf(userId);
            Set<Integer> blocked = blockListRepository.findBlockedIdsByBlockerId(userIdInt);
            Set<Integer> blockers = blockListRepository.findBlockerIdsByBlockedId(userIdInt);
            blocked.forEach(id -> excludeIds.add(id.toString()));
            blockers.forEach(id -> excludeIds.add(id.toString()));
        } catch (NumberFormatException ex) {
            log.debug("userId {} is not an integer — skipping block list lookup.", userId);
        }

        List<String> excludeIdList = new ArrayList<>(excludeIds);

        log.trace("Excluding {} total candidates (seen + blocked) for userId: {}", excludeIdList.size(), userId);

        // ── Phase 1: Hard filter ──────────────────────────────────────────────
        List<String> candidateIds = scorer.fetchCandidateIds(userId, excludeIdList);
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
            long mutualCount = matchResultRepository.countMutualLike(cognitoSubB, cognitoSubA, category, MatchStatus.LIKED);
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

                // Auto-schedule round 1 meeting for now + 3 hours
                scheduleFirstMeeting(result);
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void scheduleFirstMeeting(MatchResult match) {
        try {
            Meeting meeting = Meeting.builder()
                    .matchResultId(match.getId())
                    .roundNumber(1)
                    .scheduledAt(LocalDateTime.now().plusHours(SCHEDULE_AHEAD_HOURS))
                    .meetingType(MeetingType.SCHEDULED)
                    .status(MeetingStatus.SCHEDULED)
                    .durationMinutes(30)
                    .build();
            meetingRepository.save(meeting);
            log.info("Scheduled round-1 meeting for matchId={} at {}", match.getId(), meeting.getScheduledAt());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Failed to schedule meeting for matchId={}. Error: {}",
                    match.getId(), ex.getMessage(), ex);
        }
    }

    private void persistShownResults(String cognitoSubA, MatchCategory category,
                                     List<MatchCandidateVO> results) {
        for (MatchCandidateVO vo : results) {
            try {
                if (!matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(
                        cognitoSubA, vo.getCognitoSubB(), category)) {
                    MatchResult mr = MatchResult.builder()
                            .cognitoSubA(cognitoSubA)
                            .cognitoSubB(vo.getCognitoSubB())
                            .matchCategory(category)
                            .compatibilityScore((double) vo.getCompatibilityScore())
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
