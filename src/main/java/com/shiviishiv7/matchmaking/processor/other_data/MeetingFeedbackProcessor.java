package com.shiviishiv7.matchmaking.processor.other_data;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.feedback.FeedbackDecisionEngine;
import com.shiviishiv7.matchmaking.processor.feedback.IMeetingFeedbackProcessor;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

/**
 * Handles submission and retrieval of meeting feedback.
 * After saving, delegates to FeedbackDecisionEngine to evaluate the outcome
 * if both users have now submitted their response.
 */
@Component
@Transactional
@Slf4j
public class MeetingFeedbackProcessor implements IMeetingFeedbackProcessor {

    @Autowired
    private MeetingFeedbackRepository meetingFeedbackRepository;

    @Autowired
    private FeedbackDecisionEngine feedbackDecisionEngine;

    @Override
    public BaseVO submit(MeetingFeedbackVO vo) throws MatchmakingException {
        try {
            log.info("Submitting feedback for meetingId: {} from user: {}", vo.getMeetingId(), vo.getCognitoSub());

            if (vo.getMeetingId() == null || vo.getMeetingId().isBlank()) {
                throw new MatchmakingException("meetingId is required", VALIDATION_ERROR);
            }
            if (vo.getCognitoSub() == null || vo.getCognitoSub().isBlank()) {
                throw new MatchmakingException("cognitoSub is required", VALIDATION_ERROR);
            }
            if (vo.getResponse() == null) {
                throw new MatchmakingException("response is required", VALIDATION_ERROR);
            }

            // Prevent duplicate submission from the same user for the same meeting
            if (meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(
                    vo.getMeetingId(), vo.getCognitoSub())) {
                log.warn("Duplicate feedback submission from user: {} for meeting: {}",
                        vo.getCognitoSub(), vo.getMeetingId());
                throw new MatchmakingException(
                        "Feedback already submitted for this meeting", DUPLICATE_RECORD);
            }

            MeetingFeedback feedback = MeetingFeedback.builder()
                    .meetingId(vo.getMeetingId())
                    .cognitoSub(vo.getCognitoSub())
                    .response(vo.getResponse())
                    .notes(vo.getNotes())
                    .build();

            meetingFeedbackRepository.save(feedback);
            log.info("Feedback saved for meetingId: {} from user: {}", vo.getMeetingId(), vo.getCognitoSub());

            // Evaluate outcome — engine handles the "wait for both" check internally
            feedbackDecisionEngine.evaluate(vo.getMeetingId());

            return new BaseVO(SUCCESS, "Feedback submitted", "Feedback submitted successfully");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error submitting feedback for meeting: {}. Error: {}",
                    vo.getMeetingId(), ex.getMessage(), ex);
            throw new MatchmakingException(
                    "Error submitting feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getByMeeting(String meetingId) throws MatchmakingException {
        try {
            log.info("Fetching feedbacks for meetingId: {}", meetingId);
            if (meetingId == null || meetingId.isBlank()) {
                throw new MatchmakingException("meetingId is required", VALIDATION_ERROR);
            }
            var feedbacks = meetingFeedbackRepository.findByMeetingId(meetingId);
            return new BaseVO(SUCCESS, "Feedbacks fetched", "Feedbacks fetched", feedbacks);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching feedbacks for meeting: {}. Error: {}",
                    meetingId, ex.getMessage(), ex);
            throw new MatchmakingException(
                    "Error fetching feedbacks: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
