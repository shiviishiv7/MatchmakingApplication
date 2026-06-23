package com.shiviishiv7.matchmaking.scheduler;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.vo.ws.MeetingNotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for MeetingSchedulerJob —
 * validates the Optional.get() fix (was crashing with NoSuchElementException
 * when a MatchResult was not found, silently swallowed by catch block).
 */
@ExtendWith(MockitoExtension.class)
class MeetingSchedulerJobTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MatchResultRepository matchRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MeetingSchedulerJob job;

    // ── openWaitingRooms ───────────────────────────────────────────────────────

    @Test
    @DisplayName("openWaitingRooms: does nothing when no meetings are ready")
    void openWaitingRooms_noReadyMeetings_doesNothing() {
        when(meetingRepository.findReadyToOpenWaitingRoom(any())).thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() -> job.openWaitingRooms());

        verifyNoInteractions(matchRepository, messagingTemplate);
    }

    @Test
    @DisplayName("openWaitingRooms: skips meeting gracefully when MatchResult is missing (fix for NPE)")
    void openWaitingRooms_matchNotFound_skipsGracefully() {
        Meeting meeting = buildMeeting("42", MeetingStatus.SCHEDULED);
        when(meetingRepository.findReadyToOpenWaitingRoom(any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(any(Integer.class))).thenReturn(Optional.empty()); // match row missing

        // Before the fix this would throw NoSuchElementException from Optional.get()
        assertThatNoException().isThrownBy(() -> job.openWaitingRooms());

        // Meeting is still saved to WAITING_ROOM (happens before the match lookup)
        verify(meetingRepository).save(meeting);
        // WebSocket push should NOT happen
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("openWaitingRooms: notifies both users when match is found")
    void openWaitingRooms_matchFound_notifiesBothUsers() {
        MatchResult match = buildMatch(99, "sub-a", "sub-b", MatchStatus.CONNECTED);
        Meeting meeting = buildMeeting(String.valueOf(match.getId()), MeetingStatus.SCHEDULED);

        when(meetingRepository.findReadyToOpenWaitingRoom(any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(any(Integer.class))).thenReturn(Optional.of(match));

        job.openWaitingRooms();

        // Both users should receive a notification
        verify(messagingTemplate).convertAndSendToUser(eq("sub-a"), eq("/queue/meeting"), any(MeetingNotificationVO.class));
        verify(messagingTemplate).convertAndSendToUser(eq("sub-b"), eq("/queue/meeting"), any(MeetingNotificationVO.class));

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.WAITING_ROOM);
    }

    // ── markExpiredMeetingsCompleted ───────────────────────────────────────────

    @Test
    @DisplayName("markExpiredMeetings: does nothing when no expired meetings")
    void markExpiredMeetings_noExpired_doesNothing() {
        when(meetingRepository.findExpiredActiveMeetings(any())).thenReturn(Collections.emptyList());

        assertThatNoException().isThrownBy(() -> job.markExpiredMeetingsCompleted());

        verifyNoInteractions(matchRepository);
    }

    @Test
    @DisplayName("markExpiredMeetings: skips gracefully when MatchResult is missing (fix for NPE)")
    void markExpiredMeetings_matchNotFound_skipsGracefully() {
        Meeting meeting = buildMeeting("42", MeetingStatus.IN_PROGRESS);
        when(meetingRepository.findExpiredActiveMeetings(any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(any(Integer.class))).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> job.markExpiredMeetingsCompleted());

        // Meeting is still marked COMPLETED
        verify(meetingRepository).save(meeting);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COMPLETED);
    }

    @Test
    @DisplayName("markExpiredMeetings: transitions MEETING_SCHEDULED match to AWAITING_FEEDBACK")
    void markExpiredMeetings_scheduledMatch_transitionsToAwaitingFeedback() {
        Meeting meeting = buildMeeting("42", MeetingStatus.IN_PROGRESS);
        MatchResult match = buildMatch(99, "sub-a", "sub-b", MatchStatus.MEETING_SCHEDULED);

        when(meetingRepository.findExpiredActiveMeetings(any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(any(Integer.class))).thenReturn(Optional.of(match));

        job.markExpiredMeetingsCompleted();

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COMPLETED);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.AWAITING_FEEDBACK);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("markExpiredMeetings: does NOT change match status if match is not MEETING_SCHEDULED")
    void markExpiredMeetings_matchAlreadyConnected_doesNotChangeMatchStatus() {
        Meeting meeting = buildMeeting("42", MeetingStatus.IN_PROGRESS);
        MatchResult match = buildMatch(99, "sub-a", "sub-b", MatchStatus.CONNECTED);

        when(meetingRepository.findExpiredActiveMeetings(any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(any(Integer.class))).thenReturn(Optional.of(match));

        job.markExpiredMeetingsCompleted();

        assertThat(match.getStatus()).isEqualTo(MatchStatus.CONNECTED); // unchanged
        verify(matchRepository, never()).save(match);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Meeting buildMeeting(String matchId, MeetingStatus status) {
        return Meeting.builder()
                .id(1)
                .matchId(matchId)
                .roundNumber(1)
                .status(status)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    private MatchResult buildMatch(int id, String subA, String subB, MatchStatus status) {
        // Lombok @Builder includes the id field — @GeneratedValue only applies to JPA persistence
        return MatchResult.builder()
                .id(id)
                .cognitoSubA(subA)
                .cognitoSubB(subB)
                .matchCategory(com.shiviishiv7.matchmaking.common.enums.MatchCategory.CASUAL_DATING)
                .status(status)
                .isMutual(true)
                .shownAt(LocalDateTime.now())
                .build();
    }
}
