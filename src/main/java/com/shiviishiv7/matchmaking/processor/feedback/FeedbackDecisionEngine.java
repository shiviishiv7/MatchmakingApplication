package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.matchingengine.MatchingEngineProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.UNKNOWN_EXCEPTION;

/**
 * Evaluates both users' feedback after a meeting and advances the match state.
 *
 * Decision matrix:
 *   YES + YES, rounds remaining → ANOTHER_ROUND  (schedules next meeting)
 *   YES + YES, max rounds hit   → COMPLETED
 *   NO  + *                     → ENDED
 *
 * Called by MeetingFeedbackProcessor after every feedback submission.
 * Waits silently until both feedbacks are present before acting.
 */
@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FeedbackDecisionEngine {

    private final MeetingFeedbackRepository meetingFeedbackRepository;
    private final MeetingRepository meetingRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchingEngineProcessor matchingEngineProcessor;

    public void evaluate(String meetingId) throws MatchmakingException {
        try {
            long feedbackCount = meetingFeedbackRepository.countByMeetingId(meetingId);
            if (feedbackCount < 2) {
                log.info("Only {}/2 feedbacks for meeting {} — waiting.", feedbackCount, meetingId);
                return;
            }

            List<MeetingFeedback> feedbacks = meetingFeedbackRepository.findByMeetingId(meetingId);
            FeedbackResponse responseA = feedbacks.get(0).getResponse();
            FeedbackResponse responseB = feedbacks.get(1).getResponse();
            log.info("Both feedbacks for meeting {}: {} / {}", meetingId, responseA, responseB);

            Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(meetingId));
            if (optionalMeeting.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Meeting {} not found during feedback evaluation.", meetingId);
                return;
            }
            Meeting meeting = optionalMeeting.get();

            Optional<MatchResult> optionalMatch = matchResultRepository.findById(meeting.getMatchResultId());
            if (optionalMatch.isEmpty()) {
                log.error("ALERT_FOR_ERROR: MatchResult {} not found during feedback evaluation.",
                        meeting.getMatchResultId());
                return;
            }
            MatchResult match = optionalMatch.get();

            // Either NO → end the match
            if (responseA == FeedbackResponse.NO || responseB == FeedbackResponse.NO) {
                log.info("At least one NO — ending match {}", match.getId());
                match.setStatus(MatchStatus.ENDED);
                matchResultRepository.save(match);
                return;
            }

            // Both YES — check if more rounds remain
            int nextRound = match.getRoundCount() + 1;
            if (nextRound > match.getMaxRounds()) {
                log.info("Max rounds ({}) reached for match {} — marking COMPLETED.", match.getMaxRounds(), match.getId());
                match.setStatus(MatchStatus.COMPLETED);
                matchResultRepository.save(match);
                return;
            }

            // Schedule the next round
            log.info("Both YES — scheduling round {} for match {}", nextRound, match.getId());
            match.setRoundCount(nextRound);
            match.setStatus(MatchStatus.ANOTHER_ROUND);
            matchResultRepository.save(match);

            matchingEngineProcessor.scheduleRoundMeeting(match, nextRound);
            log.info("Round {} meeting scheduled for match {}", nextRound, match.getId());

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: FeedbackDecisionEngine failed for meeting {}: {}",
                    meetingId, ex.getMessage(), ex);
            throw new MatchmakingException("Error evaluating feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
