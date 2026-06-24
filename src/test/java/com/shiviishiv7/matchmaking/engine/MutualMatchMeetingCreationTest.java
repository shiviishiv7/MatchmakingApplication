package com.shiviishiv7.matchmaking.engine;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.BlockListRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests that a scheduled meeting (scheduledAt = now+3h) is auto-created
 * exactly when a mutual match is detected in recordAction().
 */
@ExtendWith(MockitoExtension.class)
class MutualMatchMeetingCreationTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MeetingRepository      meetingRepository;
    @Mock private BlockListRepository    blockListRepository;
    @Mock private CategoryScorer         dummyScorer;

    private MatchingEngineProcessor engine;

    private static final String USER_A = "sub-a";
    private static final String USER_B = "sub-b";
    private static final MatchCategory CAT = MatchCategory.MENTORSHIP;

    @BeforeEach
    void setUp() {
        when(dummyScorer.supports()).thenReturn(CAT);
        engine = new MatchingEngineProcessor(List.of(dummyScorer));
        injectField(engine, "matchResultRepository", matchResultRepository);
        injectField(engine, "meetingRepository",     meetingRepository);
        injectField(engine, "blockListRepository",   blockListRepository);
    }

    @Test
    @DisplayName("TC-S01: mutual match → one Meeting row saved with status SCHEDULED")
    void mutualMatch_meetingSavedWithScheduledStatus() throws Exception {
        setupMutualMatch();

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());

        Meeting saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MeetingStatus.SCHEDULED);
    }

    @Test
    @DisplayName("TC-S02: mutual match → meeting roundNumber is 1")
    void mutualMatch_meetingRoundNumberIsOne() throws Exception {
        setupMutualMatch();

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getRoundNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-S03: mutual match → scheduledAt is approximately now + 3 hours")
    void mutualMatch_scheduledAtIsThreeHoursFromNow() throws Exception {
        setupMutualMatch();
        LocalDateTime before = LocalDateTime.now();

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());

        LocalDateTime scheduledAt = captor.getValue().getScheduledAt();
        assertThat(scheduledAt).isAfterOrEqualTo(before.plusHours(3).minusSeconds(5));
        assertThat(scheduledAt).isBeforeOrEqualTo(after.plusHours(3).plusSeconds(5));
    }

    @Test
    @DisplayName("TC-S04: mutual match → matchId on Meeting equals the MatchResult id")
    void mutualMatch_meetingMatchIdMatchesMatchResultId() throws Exception {
        MatchResult aRow = buildResult(99, USER_A, USER_B, MatchStatus.PENDING);
        MatchResult bRow = buildResult(100, USER_B, USER_A, MatchStatus.LIKED);

        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(aRow));
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED)).thenReturn(1L);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_B, USER_A, CAT))
                .thenReturn(Optional.of(bRow));

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchId()).isEqualTo("99");
    }

    @Test
    @DisplayName("TC-S05: no mutual match → NO meeting is created")
    void noMutualMatch_noMeetingCreated() throws Exception {
        MatchResult aRow = buildResult(99, USER_A, USER_B, MatchStatus.PENDING);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(aRow));
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED)).thenReturn(0L);

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED);

        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("TC-S06: SKIP action → no meeting created")
    void skipAction_noMeetingCreated() throws Exception {
        MatchResult aRow = buildResult(99, USER_A, USER_B, MatchStatus.PENDING);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(aRow));

        engine.recordAction(USER_A, USER_B, CAT, MatchStatus.SKIPPED);

        verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("TC-S07: meetingRepository.save failure does not crash recordAction (graceful error handling)")
    void meetingSaveFails_doesNotCrashRecordAction() throws Exception {
        setupMutualMatch();
        doThrow(new RuntimeException("DB down")).when(meetingRepository).save(any());

        // Should not propagate — error is swallowed with a log
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> engine.recordAction(USER_A, USER_B, CAT, MatchStatus.LIKED));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void setupMutualMatch() {
        MatchResult aRow = buildResult(99, USER_A, USER_B, MatchStatus.PENDING);
        MatchResult bRow = buildResult(100, USER_B, USER_A, MatchStatus.LIKED);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_A, USER_B, CAT))
                .thenReturn(Optional.of(aRow));
        when(matchResultRepository.countMutualLike(USER_B, USER_A, CAT, MatchStatus.LIKED)).thenReturn(1L);
        when(matchResultRepository.findByCognitoSubAAndCognitoSubBAndMatchCategory(USER_B, USER_A, CAT))
                .thenReturn(Optional.of(bRow));
    }

    private MatchResult buildResult(int id, String subA, String subB, MatchStatus status) {
        return MatchResult.builder()
                .id(id).cognitoSubA(subA).cognitoSubB(subB)
                .matchCategory(CAT).status(status).isMutual(false)
                .shownAt(LocalDateTime.now()).build();
    }

    private void injectField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot inject " + name, e);
        }
    }
}
