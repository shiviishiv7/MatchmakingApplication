package com.shiviishiv7.matchmaking.engine;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingEngineProcessor.discover() —
 * verifies that the engine persists MatchResult rows and auto-schedules
 * meetings when candidates are found.
 */
@ExtendWith(MockitoExtension.class)
class MatchingEngineProcessorTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private BlockListRepository blockListRepository;
    @Mock private CategoryScorer dummyScorer;

    private MatchingEngineProcessor engine;

    private static final String USER_A = "sub-a";
    private static final String USER_B = "sub-b";
    private static final MatchCategory CAT = MatchCategory.CASUAL_DATING;

    @BeforeEach
    void setUp() {
        when(dummyScorer.supports()).thenReturn(CAT);
        engine = new MatchingEngineProcessor(List.of(dummyScorer));
        injectField(engine, "matchResultRepository", matchResultRepository);
        injectField(engine, "meetingRepository", meetingRepository);
        injectField(engine, "blockListRepository", blockListRepository);
    }

    @Test
    @DisplayName("discover: no candidates → returns empty list, no MatchResult or meeting saved")
    void discover_noCandidates_returnsEmpty() throws Exception {
        when(matchResultRepository.findSeenCandidateIds(USER_A, CAT)).thenReturn(Set.of());
        when(dummyScorer.fetchCandidateIds(eq(USER_A), any())).thenReturn(Collections.emptyList());

        List<MatchCandidateVO> result = engine.discover(buildRequest(USER_A));

        assertThat(result).isEmpty();
        verify(matchResultRepository, never()).save(any());
        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("discover: new candidate found → MatchResult saved with PENDING status")
    void discover_newCandidate_matchResultSavedAsPending() throws Exception {
        when(matchResultRepository.findSeenCandidateIds(USER_A, CAT)).thenReturn(Set.of());
        when(dummyScorer.fetchCandidateIds(eq(USER_A), any())).thenReturn(List.of(USER_B));
        when(dummyScorer.score(USER_A, USER_B)).thenReturn(buildCandidate(USER_B, 85));
        when(matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(false);

        engine.discover(buildRequest(USER_A));

        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MatchStatus.PENDING);
        assertThat(captor.getValue().getCognitoSubA()).isEqualTo(USER_A);
        assertThat(captor.getValue().getCognitoSubB()).isEqualTo(USER_B);
    }

    @Test
    @DisplayName("discover: new candidate found → no meeting scheduled (online check is deferred to MatchConnectService)")
    void discover_newCandidate_noMeetingScheduledImmediately() throws Exception {
        when(matchResultRepository.findSeenCandidateIds(USER_A, CAT)).thenReturn(Set.of());
        when(dummyScorer.fetchCandidateIds(eq(USER_A), any())).thenReturn(List.of(USER_B));
        when(dummyScorer.score(USER_A, USER_B)).thenReturn(buildCandidate(USER_B, 85));
        when(matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(false);

        engine.discover(buildRequest(USER_A));

        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("discover: candidate already matched → no duplicate MatchResult or meeting")
    void discover_alreadyMatched_noNewRowCreated() throws Exception {
        when(matchResultRepository.findSeenCandidateIds(USER_A, CAT)).thenReturn(Set.of());
        when(dummyScorer.fetchCandidateIds(eq(USER_A), any())).thenReturn(List.of(USER_B));
        when(dummyScorer.score(USER_A, USER_B)).thenReturn(buildCandidate(USER_B, 85));
        when(matchResultRepository.existsByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(true);

        engine.discover(buildRequest(USER_A));

        verify(matchResultRepository, never()).save(any());
        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("discover: already-seen candidates are excluded from scoring phase")
    void discover_seenCandidatesExcluded() throws Exception {
        when(matchResultRepository.findSeenCandidateIds(USER_A, CAT)).thenReturn(Set.of(USER_B));
        when(dummyScorer.fetchCandidateIds(eq(USER_A), argThat(list -> list.contains(USER_B))))
                .thenReturn(Collections.emptyList());

        List<MatchCandidateVO> result = engine.discover(buildRequest(USER_A));

        assertThat(result).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MatchDiscoveryRequestVO buildRequest(String userId) {
        MatchDiscoveryRequestVO req = new MatchDiscoveryRequestVO();
        req.setCognitoSubA(userId);
        req.setMatchCategory(CAT);
        req.setPage(0);
        req.setPageSize(10);
        return req;
    }

    private MatchCandidateVO buildCandidate(String subB, int score) {
        MatchCandidateVO vo = new MatchCandidateVO();
        vo.setCognitoSubB(subB);
        vo.setCompatibilityScore(score);
        return vo;
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject field: " + fieldName, e);
        }
    }
}
