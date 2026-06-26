package com.shiviishiv7.matchmaking.processor.feedback;

import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MeetingFeedbackProcessor implements IMeetingFeedbackProcessor {

    private final MeetingFeedbackRepository meetingFeedbackRepository;
    private final MeetingRepository meetingRepository;
    private final FeedbackDecisionEngine feedbackDecisionEngine;

    @Override
    public BaseVO submit(MeetingFeedbackVO feedbackVO) throws MatchmakingException {
        try {
            feedbackVO.validate();

            // Meeting must exist and be in a terminal call state
            Optional<Meeting> optionalMeeting = meetingRepository.findById(
                    Integer.valueOf(feedbackVO.getMeetingId()));
            if (optionalMeeting.isEmpty()) {
                throw new MatchmakingException("Meeting does not exist", DATA_NOT_FOUND);
            }
            Meeting meeting = optionalMeeting.get();
            if (meeting.getStatus() != MeetingStatus.COMPLETED
                    && meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
                throw new MatchmakingException(
                        "Feedback can only be submitted for a completed or in-progress meeting", VALIDATION_ERROR);
            }

            // One feedback per user per meeting
            if (meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(
                    feedbackVO.getMeetingId(), feedbackVO.getCognitoSub())) {
                throw new MatchmakingException("Feedback already submitted for this meeting", DUPLICATE_RECORD);
            }

            MeetingFeedback saved = meetingFeedbackRepository.save(feedbackVO.fromVO());
            log.info("Feedback saved: meetingId={} user={} response={}",
                    feedbackVO.getMeetingId(), feedbackVO.getCognitoSub(), feedbackVO.getResponse());

            // Evaluate once both feedbacks are in
            feedbackDecisionEngine.evaluate(feedbackVO.getMeetingId());

            return new BaseVO(SUCCESS, "Feedback submitted", "Feedback submitted",
                    new MeetingFeedbackVO().toVO(saved));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error submitting feedback: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error submitting feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO get(String id) throws MatchmakingException {
        try {
            Optional<MeetingFeedback> optional = meetingFeedbackRepository.findById(Integer.valueOf(id));
            if (optional.isEmpty()) {
                throw new MatchmakingException("Feedback not found", DATA_NOT_FOUND);
            }
            return new BaseVO(SUCCESS, "Feedback fetched", "Feedback fetched",
                    new MeetingFeedbackVO().toVO(optional.get()));
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching feedback: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAllForMeeting(String meetingId) throws MatchmakingException {
        try {
            List<MeetingFeedbackVO> result = meetingFeedbackRepository.findByMeetingId(meetingId)
                    .stream()
                    .map(f -> new MeetingFeedbackVO().toVO(f))
                    .collect(Collectors.toList());
            return new BaseVO(SUCCESS, "Feedbacks fetched", "Feedbacks fetched", result);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching feedbacks: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching feedbacks: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
