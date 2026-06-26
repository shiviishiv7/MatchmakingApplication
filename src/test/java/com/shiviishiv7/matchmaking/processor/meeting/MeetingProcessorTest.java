package com.shiviishiv7.matchmaking.processor.meeting;

import com.shiviishiv7.matchmaking.common.enums.*;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingProcessorTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MatchResultRepository matchRepository;
    @Mock private BaseUserProfileRepository userProfileRepository;

    @InjectMocks
    private MeetingProcessor processor;

    // ── add ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("add: creates meeting and transitions PENDING match to MEETING_SCHEDULED")
    void add_pendingMatch_createsAndTransitions() throws MatchmakingException {
        MatchResult match = buildMatch(1, MatchStatus.PENDING);
        MeetingVO vo = validMeetingVO(1);
        Meeting saved = buildMeeting(10, 1);

        when(matchRepository.findById(1)).thenReturn(Optional.of(match));
        when(meetingRepository.save(any())).thenReturn(saved);

        BaseVO result = processor.add(vo);

        assertThat(result).isNotNull();
        assertThat(match.getStatus()).isEqualTo(MatchStatus.MEETING_SCHEDULED);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("add: creates meeting and transitions ANOTHER_ROUND match to MEETING_SCHEDULED")
    void add_anotherRoundMatch_createsAndTransitions() throws MatchmakingException {
        MatchResult match = buildMatch(2, MatchStatus.ANOTHER_ROUND);
        MeetingVO vo = validMeetingVO(2);
        Meeting saved = buildMeeting(20, 2);

        when(matchRepository.findById(2)).thenReturn(Optional.of(match));
        when(meetingRepository.save(any())).thenReturn(saved);

        processor.add(vo);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.MEETING_SCHEDULED);
    }

    @Test
    @DisplayName("add: does NOT change match status when already ENDED (terminal state)")
    void add_endedMatch_doesNotChangeStatus() throws MatchmakingException {
        MatchResult match = buildMatch(3, MatchStatus.ENDED);
        MeetingVO vo = validMeetingVO(3);
        Meeting saved = buildMeeting(30, 3);

        when(matchRepository.findById(3)).thenReturn(Optional.of(match));
        when(meetingRepository.save(any())).thenReturn(saved);

        processor.add(vo);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
        verify(matchRepository, never()).save(match);
    }

    @Test
    @DisplayName("add: throws when match does not exist")
    void add_matchNotFound_throws() {
        MeetingVO vo = validMeetingVO(99);
        when(matchRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.add(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Match does not exist");
    }

    @Test
    @DisplayName("add: throws when matchResultId is null (validation wrapped in MatchmakingException)")
    void add_nullMatchResultId_throws() {
        MeetingVO vo = new MeetingVO();
        vo.setRoundNumber(1);
        vo.setScheduledAt(LocalDateTime.now().plusDays(1));
        vo.setMeetingType(MeetingType.SCHEDULED);

        assertThatThrownBy(() -> processor.add(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Match result ID cannot be null");
    }

    @Test
    @DisplayName("add: throws when roundNumber is zero (validation wrapped in MatchmakingException)")
    void add_invalidRoundNumber_throws() {
        MeetingVO vo = new MeetingVO();
        vo.setMatchResultId(1);
        vo.setRoundNumber(0);
        vo.setScheduledAt(LocalDateTime.now().plusDays(1));
        vo.setMeetingType(MeetingType.SCHEDULED);

        assertThatThrownBy(() -> processor.add(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Round number");
    }

    @Test
    @DisplayName("add: throws when scheduledAt is null for SCHEDULED meeting (validation wrapped in MatchmakingException)")
    void add_nullScheduledAt_throws() {
        MeetingVO vo = new MeetingVO();
        vo.setMatchResultId(1);
        vo.setRoundNumber(1);
        vo.setMeetingType(MeetingType.SCHEDULED);
        // scheduledAt intentionally null

        assertThatThrownBy(() -> processor.add(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Scheduled time");
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get: returns meeting when found")
    void get_meetingExists_returns() throws MatchmakingException {
        Meeting meeting = buildMeeting(5, 1);
        when(meetingRepository.findById(5)).thenReturn(Optional.of(meeting));

        BaseVO result = processor.get("5");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("get: throws when meeting not found")
    void get_meetingNotFound_throws() {
        when(meetingRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.get("99"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Meeting does not exist");
    }

    // ── getAllForMatch ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllForMatch: returns list when meetings exist")
    void getAllForMatch_meetingsExist_returnsList() throws MatchmakingException {
        List<Meeting> meetings = List.of(buildMeeting(1, 10), buildMeeting(2, 10));
        when(meetingRepository.findByMatchResultId(10)).thenReturn(meetings);

        BaseVO result = processor.getAllForMatch("10");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAllForMatch: throws when no meetings exist for match")
    void getAllForMatch_noMeetings_throws() {
        when(meetingRepository.findByMatchResultId(10)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> processor.getAllForMatch("10"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("No meetings found");
    }

    // ── markCompleted ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("markCompleted: transitions SCHEDULED meeting to COMPLETED")
    void markCompleted_scheduledMeeting_setsCompleted() throws MatchmakingException {
        Meeting meeting = buildMeetingWithStatus(5, MeetingStatus.SCHEDULED);
        when(meetingRepository.findById(5)).thenReturn(Optional.of(meeting));

        processor.markCompleted("5");

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COMPLETED);
        verify(meetingRepository).save(meeting);
    }

    @Test
    @DisplayName("markCompleted: transitions IN_PROGRESS meeting to COMPLETED")
    void markCompleted_inProgressMeeting_setsCompleted() throws MatchmakingException {
        Meeting meeting = buildMeetingWithStatus(6, MeetingStatus.IN_PROGRESS);
        when(meetingRepository.findById(6)).thenReturn(Optional.of(meeting));

        processor.markCompleted("6");

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COMPLETED);
    }

    @Test
    @DisplayName("markCompleted: throws when meeting is already COMPLETED")
    void markCompleted_alreadyCompleted_throws() {
        Meeting meeting = buildMeetingWithStatus(7, MeetingStatus.COMPLETED);
        when(meetingRepository.findById(7)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> processor.markCompleted("7"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("markCompleted: throws when meeting is CANCELLED")
    void markCompleted_cancelled_throws() {
        Meeting meeting = buildMeetingWithStatus(8, MeetingStatus.CANCELLED);
        when(meetingRepository.findById(8)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> processor.markCompleted("8"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("markCompleted: throws when meeting not found")
    void markCompleted_notFound_throws() {
        when(meetingRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.markCompleted("99"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Meeting does not exist");
    }

    // ── getUpcomingMeetings ───────────────────────────────────────────────────

    @Test
    @DisplayName("getUpcomingMeetings: returns empty list when no upcoming meetings")
    void getUpcomingMeetings_none_returnsEmpty() throws MatchmakingException {
        when(meetingRepository.findUpcomingForUser(anyString(), any())).thenReturn(Collections.emptyList());

        BaseVO result = processor.getUpcomingMeetings("sub-user-1");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getUpcomingMeetings: populates peer info from match and user profile")
    void getUpcomingMeetings_withMatch_populatesPeerInfo() throws MatchmakingException {
        String userSub = "sub-user-1";
        String peerSub = "sub-user-2";
        Meeting meeting = buildMeeting(1, 10);
        MatchResult match = buildMatch(10, MatchStatus.MEETING_SCHEDULED);
        match.setCognitoSubA(userSub);
        match.setCognitoSubB(peerSub);
        BaseUserProfile peer = BaseUserProfile.builder()
                .cognitoSub(peerSub)
                .name("Jane")
                .email("jane@test.com")
                .build();

        when(meetingRepository.findUpcomingForUser(eq(userSub), any())).thenReturn(List.of(meeting));
        when(matchRepository.findById(10)).thenReturn(Optional.of(match));
        when(userProfileRepository.findByCognitoSub(peerSub)).thenReturn(Optional.of(peer));

        BaseVO result = processor.getUpcomingMeetings(userSub);

        assertThat(result).isNotNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MeetingVO validMeetingVO(int matchResultId) {
        MeetingVO vo = new MeetingVO();
        vo.setMatchResultId(matchResultId);
        vo.setRoundNumber(1);
        vo.setScheduledAt(LocalDateTime.now().plusDays(1));
        vo.setMeetingType(MeetingType.SCHEDULED);
        vo.setDurationMinutes(30);
        return vo;
    }

    private Meeting buildMeeting(int id, int matchResultId) {
        return Meeting.builder()
                .id(id)
                .matchResultId(matchResultId)
                .roundNumber(1)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(MeetingStatus.SCHEDULED)
                .meetingType(MeetingType.SCHEDULED)
                .build();
    }

    private Meeting buildMeetingWithStatus(int id, MeetingStatus status) {
        return Meeting.builder()
                .id(id)
                .matchResultId(1)
                .roundNumber(1)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(status)
                .meetingType(MeetingType.SCHEDULED)
                .build();
    }

    private MatchResult buildMatch(int id, MatchStatus status) {
        return MatchResult.builder()
                .id(id)
                .cognitoSubA("sub-a")
                .cognitoSubB("sub-b")
                .matchCategory(MatchCategory.CASUAL_DATING)
                .status(status)
                .build();
    }
}
