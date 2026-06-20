package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.UNKNOWN_EXCEPTION;

@Component
@Transactional
@Slf4j
public class FeedbackDecisionEngine {

    @Autowired
    private MeetingFeedbackRepository meetingFeedbackRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchRepository matchRepository;

    /**
     * Called after every feedback submission.
     * If both users have submitted feedback for the meeting, evaluates the outcome
     * and transitions the match state accordingly.
     */
    public void evaluate(String meetingId) throws MatchmakingException {
        try {
            long feedbackCount = meetingFeedbackRepository.countByMeetingId(meetingId);
            if (feedbackCount < 2) {
                log.info("Only {}/2 feedbacks received for meeting ID: {}. Waiting for the other user.", feedbackCount, meetingId);
                return;
            }

            log.info("Both feedbacks received for meeting ID: {}. Evaluating decision.", meetingId);

            List<MeetingFeedback> feedbacks = meetingFeedbackRepository.findByMeetingId(meetingId);
            String meetingId1 = feedbacks.get(0).getMeetingId();
            Optional<Match> optionalMatch = null;// matchRepository.findByMee(meetingId);
            Match match = optionalMatch.get();

            FeedbackResponse responseA = feedbacks.get(0).getResponse();
            FeedbackResponse responseB = feedbacks.get(1).getResponse();

            log.info("Feedback responses — User A: {}, User B: {} for match ID: {}", responseA, responseB, match.getId());

            // Either not interested → end the match
            if (responseA == FeedbackResponse.NOT_INTERESTED || responseB == FeedbackResponse.NOT_INTERESTED) {
                log.info("At least one user is not interested. Ending match ID: {}", match.getId());
                match.setStatus(MatchStatus.ENDED);
                matchRepository.save(match);
                return;
            }

            // Both interested → complete the match
            if (responseA == FeedbackResponse.INTERESTED && responseB == FeedbackResponse.INTERESTED) {
                log.info("Both users are interested. Marking match ID: {} as COMPLETED.", match.getId());
                match.setStatus(MatchStatus.COMPLETED);
                matchRepository.save(match);
                return;
            }

            // At least one wants another round
            int nextRound = match.getRoundCount() + 1;
            if (nextRound >= match.getMaxRounds()) {
                log.info("Max rounds ({}) reached for match ID: {}. Ending match.", match.getMaxRounds(), match.getId());
                match.setStatus(MatchStatus.ENDED);
                matchRepository.save(match);
                return;
            }

            log.info("Scheduling round {} for match ID: {}", nextRound + 1, match.getId());
            match.setRoundCount(nextRound);
            match.setStatus(MatchStatus.ANOTHER_ROUND);
            matchRepository.save(match);

            // Schedule the next meeting 7 days from now (Zoom link populated separately)
            Meeting nextMeeting = Meeting.builder()
                    .matchId(match.getId().toString())
                    .roundNumber(nextRound + 1)
                    .scheduledAt(LocalDateTime.now().plusDays(7))
                    .durationMinutes(30)
                    .status(MeetingStatus.SCHEDULED)
                    .meetingType(MeetingType.SCHEDULED)
                    .build();
            meetingRepository.save(nextMeeting);
            log.info("Next meeting (round {}) scheduled for match ID: {}", nextRound + 1, match.getId());

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred in FeedbackDecisionEngine for meeting ID: {}. Error: {}", meetingId, ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while evaluating feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
