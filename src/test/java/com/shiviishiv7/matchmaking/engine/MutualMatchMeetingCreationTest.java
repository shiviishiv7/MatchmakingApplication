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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests that the engine auto-schedules a meeting immediately when
 * a new MatchResult is created (engine-assigned flow, no user swiping).
 */
@ExtendWith(MockitoExtension.class)
class MutualMatchMeetingCreationTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MeetingRepository      meetingRepository;
    @Mock private BlockListRepository    blockListRepository;
    @Mock private CategoryScorer         dummyScorer;

    private MatchingEngineProcessor engine;

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
    @DisplayName("scheduleRoundMeeting: meeting saved with SCHEDULED status")
    void scheduleRoundMeeting_statusIsScheduled() {
        MatchResult match = buildMatch(99);

        engine.scheduleRoundMeeting(match, 1);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingStatus.SCHEDULED);
    }

    @Test
    @DisplayName("scheduleRoundMeeting: round number is passed through correctly")
    void scheduleRoundMeeting_roundNumberSetCorrectly() {
        MatchResult match = buildMatch(99);

        engine.scheduleRoundMeeting(match, 2);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getRoundNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("scheduleRoundMeeting: scheduledAt is approximately now + 3 hours")
    void scheduleRoundMeeting_scheduledAtIsThreeHoursFromNow() {
        LocalDateTime before = LocalDateTime.now();
        MatchResult match = buildMatch(99);

        engine.scheduleRoundMeeting(match, 1);

        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());

        LocalDateTime scheduledAt = captor.getValue().getScheduledAt();
        assertThat(scheduledAt).isAfterOrEqualTo(before.plusHours(3).minusSeconds(5));
        assertThat(scheduledAt).isBeforeOrEqualTo(after.plusHours(3).plusSeconds(5));
    }

    @Test
    @DisplayName("scheduleRoundMeeting: matchResultId on meeting equals the MatchResult id")
    void scheduleRoundMeeting_matchResultIdLinkedCorrectly() {
        MatchResult match = buildMatch(42);

        engine.scheduleRoundMeeting(match, 1);

        ArgumentCaptor<Meeting> captor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(captor.capture());
        assertThat(captor.getValue().getMatchResultId()).isEqualTo(42);
    }

    @Test
    @DisplayName("scheduleRoundMeeting: meetingRepository failure does not propagate (graceful error handling)")
    void scheduleRoundMeeting_saveFails_doesNotThrow() {
        doThrow(new RuntimeException("DB down")).when(meetingRepository).save(any());
        MatchResult match = buildMatch(99);

        assertThatNoException().isThrownBy(() -> engine.scheduleRoundMeeting(match, 1));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private MatchResult buildMatch(int id) {
        return MatchResult.builder()
                .id(id)
                .cognitoSubA("sub-a")
                .cognitoSubB("sub-b")
                .matchCategory(CAT)
                .status(MatchStatus.MEETING_SCHEDULED)
                .build();
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
