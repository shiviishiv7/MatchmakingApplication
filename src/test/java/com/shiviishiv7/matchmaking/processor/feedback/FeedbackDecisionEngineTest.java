package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.enums.*;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackDecisionEngineTest {

    @Mock private MeetingFeedbackRepository feedbackRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private MatchResultRepository matchResultRepository;
    @Mock private MatchingEngineProcessor matchingEngineProcessor;

    @InjectMocks private FeedbackDecisionEngine engine;

    private static final String MEETING_ID = "10";

    // ── wait for second feedback ───────────────────────────────────────────────

    @Test
    @DisplayName("Only one feedback submitted — engine waits, no match update")
    void oneFeedback_waits() throws MatchmakingException {
        when(feedbackRepository.countByMeetingId(MEETING_ID)).thenReturn(1L);

        engine.evaluate(MEETING_ID);

        verifyNoInteractions(meetingRepository, matchResultRepository, matchingEngineProcessor);
    }

    // ── either NO → ENDED ─────────────────────────────────────────────────────

    @Test
    @DisplayName("One user says NO → match ENDED")
    void oneNo_matchEnded() throws MatchmakingException {
        setupFeedbacks(FeedbackResponse.YES, FeedbackResponse.NO);
        MatchResult match = buildMatch(1, 2, 3);
        setupMeeting(match);

        engine.evaluate(MEETING_ID);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
        verify(matchResultRepository).save(match);
        verifyNoInteractions(matchingEngineProcessor);
    }

    @Test
    @DisplayName("Both say NO → match ENDED")
    void bothNo_matchEnded() throws MatchmakingException {
        setupFeedbacks(FeedbackResponse.NO, FeedbackResponse.NO);
        MatchResult match = buildMatch(1, 2, 3);
        setupMeeting(match);

        engine.evaluate(MEETING_ID);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.ENDED);
    }

    // ── both YES, rounds remaining → ANOTHER_ROUND ────────────────────────────

    @Test
    @DisplayName("Both YES, rounds remaining → ANOTHER_ROUND and next meeting scheduled")
    void bothYes_roundsRemaining_schedulesNextMeeting() throws MatchmakingException {
        setupFeedbacks(FeedbackResponse.YES, FeedbackResponse.YES);
        MatchResult match = buildMatch(1, 1, 3); // roundCount=1, maxRounds=3 → nextRound=2, 2>3=false
        setupMeeting(match);

        engine.evaluate(MEETING_ID);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.ANOTHER_ROUND);
        assertThat(match.getRoundCount()).isEqualTo(2);
        verify(matchingEngineProcessor).scheduleRoundMeeting(match, 2);
    }

    // ── both YES, max rounds hit → COMPLETED ──────────────────────────────────

    @Test
    @DisplayName("Both YES but max rounds reached → COMPLETED, no new meeting")
    void bothYes_maxRoundsReached_completed() throws MatchmakingException {
        setupFeedbacks(FeedbackResponse.YES, FeedbackResponse.YES);
        MatchResult match = buildMatch(3, 3, 3); // roundCount=3 == maxRounds=3
        setupMeeting(match);

        engine.evaluate(MEETING_ID);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        verifyNoInteractions(matchingEngineProcessor);
    }

    // ── missing meeting / match → silent skip ─────────────────────────────────

    @Test
    @DisplayName("Meeting not found → no crash, no state change")
    void meetingNotFound_silentSkip() throws MatchmakingException {
        setupFeedbacks(FeedbackResponse.YES, FeedbackResponse.YES);
        when(meetingRepository.findById(Integer.valueOf(MEETING_ID))).thenReturn(Optional.empty());

        engine.evaluate(MEETING_ID);

        verifyNoInteractions(matchResultRepository, matchingEngineProcessor);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setupFeedbacks(FeedbackResponse r1, FeedbackResponse r2) {
        when(feedbackRepository.countByMeetingId(MEETING_ID)).thenReturn(2L);
        when(feedbackRepository.findByMeetingId(MEETING_ID)).thenReturn(List.of(
                MeetingFeedback.builder().meetingId(MEETING_ID).cognitoSub("sub-a").response(r1).build(),
                MeetingFeedback.builder().meetingId(MEETING_ID).cognitoSub("sub-b").response(r2).build()
        ));
    }

    private void setupMeeting(MatchResult match) {
        Meeting meeting = Meeting.builder()
                .id(Integer.parseInt(MEETING_ID))
                .matchResultId(match.getId())
                .roundNumber(match.getRoundCount())
                .status(MeetingStatus.COMPLETED)
                .build();
        when(meetingRepository.findById(Integer.valueOf(MEETING_ID))).thenReturn(Optional.of(meeting));
        when(matchResultRepository.findById(match.getId())).thenReturn(Optional.of(match));
    }

    private MatchResult buildMatch(int id, int roundCount, int maxRounds) {
        return MatchResult.builder()
                .id(id)
                .cognitoSubA("sub-a")
                .cognitoSubB("sub-b")
                .matchCategory(MatchCategory.CASUAL_DATING)
                .status(MatchStatus.AWAITING_FEEDBACK)
                .roundCount(roundCount)
                .maxRounds(maxRounds)
                .build();
    }
}
