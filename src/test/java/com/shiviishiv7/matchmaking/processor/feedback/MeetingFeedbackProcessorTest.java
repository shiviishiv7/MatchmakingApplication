package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingFeedbackProcessorTest {

    @Mock private MeetingFeedbackRepository meetingFeedbackRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private FeedbackDecisionEngine feedbackDecisionEngine;

    @InjectMocks private MeetingFeedbackProcessor processor;

    private static final String MEETING_ID = "10";
    private static final String USER_SUB   = "sub-a";

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit: saves feedback and calls decision engine for a COMPLETED meeting")
    void submit_completedMeeting_savesAndEvaluates() throws MatchmakingException {
        setupMeeting(MeetingStatus.COMPLETED);
        when(meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(MEETING_ID, USER_SUB)).thenReturn(false);
        MeetingFeedback saved = buildFeedback();
        when(meetingFeedbackRepository.save(any())).thenReturn(saved);

        processor.submit(buildVO(FeedbackResponse.YES));

        verify(meetingFeedbackRepository).save(any());
        verify(feedbackDecisionEngine).evaluate(MEETING_ID);
    }

    @Test
    @DisplayName("submit: also accepts feedback for IN_PROGRESS meeting")
    void submit_inProgressMeeting_savesAndEvaluates() throws MatchmakingException {
        setupMeeting(MeetingStatus.IN_PROGRESS);
        when(meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(MEETING_ID, USER_SUB)).thenReturn(false);
        when(meetingFeedbackRepository.save(any())).thenReturn(buildFeedback());

        processor.submit(buildVO(FeedbackResponse.NO));

        verify(feedbackDecisionEngine).evaluate(MEETING_ID);
    }

    @Test
    @DisplayName("submit: throws when meeting is in SCHEDULED state (not completable)")
    void submit_scheduledMeeting_throws() {
        setupMeeting(MeetingStatus.SCHEDULED);

        assertThatThrownBy(() -> processor.submit(buildVO(FeedbackResponse.YES)))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("completed or in-progress");
    }

    @Test
    @DisplayName("submit: throws when meeting is CANCELLED")
    void submit_cancelledMeeting_throws() {
        setupMeeting(MeetingStatus.CANCELLED);

        assertThatThrownBy(() -> processor.submit(buildVO(FeedbackResponse.YES)))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("completed or in-progress");
    }

    @Test
    @DisplayName("submit: throws DUPLICATE_RECORD when user already submitted feedback")
    void submit_duplicate_throws() {
        setupMeeting(MeetingStatus.COMPLETED);
        when(meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(MEETING_ID, USER_SUB)).thenReturn(true);

        assertThatThrownBy(() -> processor.submit(buildVO(FeedbackResponse.YES)))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    @DisplayName("submit: throws when meeting does not exist")
    void submit_meetingNotFound_throws() {
        when(meetingRepository.findById(Integer.valueOf(MEETING_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.submit(buildVO(FeedbackResponse.YES)))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Meeting does not exist");
    }

    @Test
    @DisplayName("submit: validates VO — wraps blank meetingId as MatchmakingException")
    void submit_blankMeetingId_throws() {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setMeetingId("");
        vo.setCognitoSub(USER_SUB);
        vo.setResponse(FeedbackResponse.YES);

        assertThatThrownBy(() -> processor.submit(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("meetingId is required");
    }

    @Test
    @DisplayName("submit: validates VO — wraps null response as MatchmakingException")
    void submit_nullResponse_throws() {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setMeetingId(MEETING_ID);
        vo.setCognitoSub(USER_SUB);
        vo.setResponse(null);

        assertThatThrownBy(() -> processor.submit(vo))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("response is required");
    }

    // ── get ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("get: returns feedback when found")
    void get_found_returnsVO() throws MatchmakingException {
        when(meetingFeedbackRepository.findById(1)).thenReturn(Optional.of(buildFeedback()));
        BaseVO result = processor.get("1");
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("get: throws DATA_NOT_FOUND when feedback missing")
    void get_notFound_throws() {
        when(meetingFeedbackRepository.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> processor.get("99"))
                .isInstanceOf(MatchmakingException.class)
                .hasMessageContaining("Feedback not found");
    }

    // ── getAllForMeeting ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllForMeeting: returns list (empty is valid)")
    void getAllForMeeting_returnsList() throws MatchmakingException {
        when(meetingFeedbackRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(buildFeedback()));
        BaseVO result = processor.getAllForMeeting(MEETING_ID);
        assertThat(result.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("getAllForMeeting: returns empty list without throwing")
    void getAllForMeeting_empty_noThrow() throws MatchmakingException {
        when(meetingFeedbackRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of());
        BaseVO result = processor.getAllForMeeting(MEETING_ID);
        assertThat(result).isNotNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setupMeeting(MeetingStatus status) {
        Meeting meeting = Meeting.builder()
                .id(Integer.parseInt(MEETING_ID))
                .matchResultId(5)
                .roundNumber(1)
                .scheduledAt(LocalDateTime.now().minusMinutes(35))
                .meetingType(MeetingType.SCHEDULED)
                .status(status)
                .build();
        when(meetingRepository.findById(Integer.valueOf(MEETING_ID))).thenReturn(Optional.of(meeting));
    }

    private MeetingFeedbackVO buildVO(FeedbackResponse response) {
        MeetingFeedbackVO vo = new MeetingFeedbackVO();
        vo.setMeetingId(MEETING_ID);
        vo.setCognitoSub(USER_SUB);
        vo.setResponse(response);
        return vo;
    }

    private MeetingFeedback buildFeedback() {
        return MeetingFeedback.builder()
                .id(1)
                .meetingId(MEETING_ID)
                .cognitoSub(USER_SUB)
                .response(FeedbackResponse.YES)
                .build();
    }
}
