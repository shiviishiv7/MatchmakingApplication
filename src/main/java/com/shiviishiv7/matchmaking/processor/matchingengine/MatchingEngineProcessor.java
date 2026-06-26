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
 *                           and immediately schedule round-1 meetings.
 *
 * New categories are supported automatically by adding a new CategoryScorer bean.
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

    @Autowired
    public MatchingEngineProcessor(List<CategoryScorer> scorers) {
        for (CategoryScorer scorer : scorers) {
            scorerRegistry.put(scorer.supports(), scorer);
            log.info("Registered scorer for category: {}", scorer.supports());
        }
    }

    /**
     * Main entry point — called by MatchingProcessor after a user submits a post.
     * Finds scored candidates, persists MatchResult rows, and schedules a round-1
     * meeting for each new match immediately.
     */
    public List<MatchCandidateVO> discover(MatchDiscoveryRequestVO request) throws MatchmakingException {
        String userId         = request.getCognitoSubA();
        MatchCategory category = request.getMatchCategory();
        int page              = request.getPage();
        int pageSize          = request.getPageSize();

        log.info("Discovery request: userId={} category={} page={} pageSize={}",
                userId, category, page, pageSize);

        CategoryScorer scorer = scorerRegistry.get(category);
        if (scorer == null) {
            log.error("No scorer registered for category: {}", category);
            throw new MatchmakingException("Matching not supported for category: " + category, VALIDATION_ERROR);
        }

        // Build exclude list: already-matched candidates + blocked users
        Set<String> seenIds = matchResultRepository.findSeenCandidateIds(userId, category);
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

        // Phase 1: Hard filter
        List<String> candidateIds = scorer.fetchCandidateIds(userId, excludeIdList);
        log.info("Phase 1 complete: {} candidates after hard filter for userId: {}", candidateIds.size(), userId);

        if (candidateIds.isEmpty()) return Collections.emptyList();

        // Phase 2: Score each candidate
        List<MatchCandidateVO> scored = new ArrayList<>();
        for (String candidateId : candidateIds) {
            try {
                scored.add(scorer.score(userId, candidateId));
            } catch (Exception ex) {
                log.warn("Scoring failed for candidate {} — skipping. Error: {}", candidateId, ex.getMessage());
            }
        }
        log.info("Phase 2 complete: {} candidates scored for userId: {}", scored.size(), userId);

        // Phase 3: Sort, paginate, persist + auto-schedule meeting
        scored.sort(Comparator.comparingInt(MatchCandidateVO::getCompatibilityScore).reversed());

        int fromIndex = page * pageSize;
        if (fromIndex >= scored.size()) return Collections.emptyList();
        List<MatchCandidateVO> pageResults = scored.subList(fromIndex, Math.min(fromIndex + pageSize, scored.size()));

        persistMatches(userId, category, pageResults);

        log.info("Discovery complete: returning {} results for userId={} category={}",
                pageResults.size(), userId, category);

        return pageResults;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void persistMatches(String cognitoSubA, MatchCategory category,
                               List<MatchCandidateVO> results) {
        for (MatchCandidateVO vo : results) {
            try {
                if (matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(
                        cognitoSubA, vo.getCognitoSubB(), category)) {
                    continue;
                }
                MatchResult mr = MatchResult.builder()
                        .cognitoSubA(cognitoSubA)
                        .cognitoSubB(vo.getCognitoSubB())
                        .matchCategory(category)
                        .compatibilityScore((double) vo.getCompatibilityScore())
                        .scoreBreakdown(vo.getScoreBreakdown())
                        .status(MatchStatus.PENDING)
                        .build();
                matchResultRepository.save(mr);
            } catch (Exception ex) {
                log.warn("Could not persist match for candidateUserId: {}. Error: {}",
                        vo.getCognitoSubB(), ex.getMessage());
            }
        }
    }

    public void scheduleRoundMeeting(MatchResult match, int roundNumber) {
        try {
            Meeting meeting = Meeting.builder()
                    .matchResultId(match.getId())
                    .roundNumber(roundNumber)
                    .scheduledAt(LocalDateTime.now().plusHours(SCHEDULE_AHEAD_HOURS))
                    .meetingType(MeetingType.SCHEDULED)
                    .status(MeetingStatus.SCHEDULED)
                    .durationMinutes(30)
                    .build();
            meetingRepository.save(meeting);
            log.info("Scheduled round-{} meeting for matchId={} at {}",
                    roundNumber, match.getId(), meeting.getScheduledAt());
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Failed to schedule meeting for matchId={}. Error: {}",
                    match.getId(), ex.getMessage(), ex);
        }
    }
}
