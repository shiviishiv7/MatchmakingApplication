package com.shiviishiv7.matchmaking.processor.other_data;

import com.shiviishiv7.matchmaking.common.enums.FeedbackResponse;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

/**
 * Evaluates both users' feedback after a meeting and transitions
 * the MatchResult to the correct next state.
 *
 * Decision matrix:
 *   INTERESTED    + INTERESTED    → COMPLETED       (match fulfilled)
 *   INTERESTED    + ANOTHER_ROUND → ANOTHER_ROUND   (conservative — schedule next round)
 *   ANOTHER_ROUND + ANOTHER_ROUND → ANOTHER_ROUND   (both want another round)
 *   Either        + NOT_INTERESTED → ENDED          (at least one is done)
 *   Max rounds reached             → ENDED          (no more rounds allowed)
 *
 * BUG FIXED (original code):
 *   optionalMatchResult was set to null then .get() was called → guaranteed NPE.
 *   Fixed by using MeetingRepository to look up the meeting, then using
 *   matchId from the meeting to fetch the MatchResult.
 *
 * BUG FIXED (ScheduledMeetingWaitingRoomController):
 *   subB was set to match.getCognitoSubA() instead of getCognitoSubB() — fixed there too.
 */
@Component
@Transactional
@Slf4j
public class FeedbackDecisionEngine {

    @Autowired
    private MeetingFeedbackRepository meetingFeedbackRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    /**
     * Called after every feedback submission.
     * Waits until both users have submitted before evaluating.
     */
    public void evaluate(String meetingId) throws MatchmakingException {
        try {
            long feedbackCount = meetingFeedbackRepository.countByMeetingId(meetingId);
            if (feedbackCount < 2) {
                log.info("Only {}/2 feedbacks received for meeting ID: {}. Waiting for the other user.",
                        feedbackCount, meetingId);
                return;
            }

            log.info("Both feedbacks received for meeting ID: {}. Evaluating decision.", meetingId);

            // ── Resolve meeting → MatchResult ─────────────────────────────────
            Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(meetingId));
            if (optionalMeeting.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Meeting not found for ID: {}", meetingId);
                throw new MatchmakingException("Meeting does not exist", DATA_NOT_FOUND);
            }
            Meeting meeting = optionalMeeting.get();

            Optional<MatchResult> optionalMatchResult = matchResultRepository.findById(
                    Integer.valueOf(meeting.getMatchId()));
            if (optionalMatchResult.isEmpty()) {
                log.error("ALERT_FOR_ERROR: MatchResult not found for matchId: {}", meeting.getMatchId());
                throw new MatchmakingException("MatchResult does not exist", DATA_NOT_FOUND);
            }
            MatchResult matchResult = optionalMatchResult.get();

            // ── Read both feedbacks ───────────────────────────────────────────
            List<MeetingFeedback> feedbacks = meetingFeedbackRepository.findByMeetingId(meetingId);
            FeedbackResponse responseA = feedbacks.get(0).getResponse();
            FeedbackResponse responseB = feedbacks.get(1).getResponse();

            log.info("Feedback — userA: {} | userB: {} for MatchResult ID: {}",
                    responseA, responseB, matchResult.getId());

            // ── Decision matrix ───────────────────────────────────────────────

            // Either not interested → end immediately
            if (responseA == FeedbackResponse.NOT_INTERESTED
                    || responseB == FeedbackResponse.NOT_INTERESTED) {
                log.info("At least one user NOT_INTERESTED. Ending MatchResult ID: {}", matchResult.getId());
                matchResult.setStatus(MatchStatus.ENDED);
                matchResultRepository.save(matchResult);
                return;
            }

            // Both interested → completed
            if (responseA == FeedbackResponse.INTERESTED
                    && responseB == FeedbackResponse.INTERESTED) {
                log.info("Both INTERESTED. Marking MatchResult ID: {} as COMPLETED.", matchResult.getId());
                matchResult.setStatus(MatchStatus.COMPLETED);
                matchResultRepository.save(matchResult);
                return;
            }

            // At least one wants ANOTHER_ROUND — check if max rounds reached
            int nextRound = matchResult.getRoundCount() + 1;
            if (nextRound >= matchResult.getMaxRounds()) {
                log.info("Max rounds ({}) reached for MatchResult ID: {}. Ending.",
                        matchResult.getMaxRounds(), matchResult.getId());
                matchResult.setStatus(MatchStatus.ENDED);
                matchResultRepository.save(matchResult);
                return;
            }

            // Schedule next round — fixed slot 7 days from now at 7 PM
            log.info("Scheduling round {} for MatchResult ID: {}", nextRound + 1, matchResult.getId());
            matchResult.setRoundCount(nextRound);
            matchResult.setStatus(MatchStatus.ANOTHER_ROUND);
            matchResultRepository.save(matchResult);

            LocalDateTime nextSlot = LocalDateTime.now()
                    .plusDays(7)
                    .withHour(19)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            Meeting nextMeeting = Meeting.builder()
                    .matchId(matchResult.getId().toString())
                    .roundNumber(nextRound + 1)
                    .scheduledAt(nextSlot)
                    .durationMinutes(30)
                    .status(MeetingStatus.SCHEDULED)
                    .meetingType(MeetingType.SCHEDULED)
                    .build();
            meetingRepository.save(nextMeeting);

            log.info("Next meeting (round {}) scheduled at {} for MatchResult ID: {}",
                    nextRound + 1, nextSlot, matchResult.getId());

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: FeedbackDecisionEngine failed for meeting ID: {}. Error: {}",
                    meetingId, ex.getMessage(), ex);
            throw new MatchmakingException(
                    "Error occurred while evaluating feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
