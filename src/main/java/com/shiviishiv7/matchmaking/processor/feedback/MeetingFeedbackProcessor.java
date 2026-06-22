//package com.shiviishiv7.matchmaking.processor.feedback;
//
//import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
//import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
//import com.shiviishiv7.matchmaking.provider.implementation.MeetingFeedbackRepository;
//import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
//import com.shiviishiv7.matchmaking.provider.model.Meeting;
//import com.shiviishiv7.matchmaking.provider.model.MeetingFeedback;
//import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
//import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
//import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Optional;
//
//import java.util.stream.Collectors;
//
//import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;
//
//@Component
//@Transactional
//@Slf4j
//public class MeetingFeedbackProcessor implements IMeetingFeedbackProcessor {
//
//    @Autowired
//    private MeetingFeedbackRepository meetingFeedbackRepository;
//
//    @Autowired
//    private MeetingRepository meetingRepository;
//
//    @Autowired
//    private BaseUserProfileRepository baseUserProfileRepository;
//
//    @Autowired
//    private FeedbackDecisionEngine feedbackDecisionEngine;
//
//    @Override
//    public BaseVO add(MeetingFeedbackVO feedbackVO) throws MatchmakingException {
//        try {
//            log.info("Validating inputs for meeting feedback creation.");
//            feedbackVO.validate();
//            log.info("MeetingFeedbackVO validation completed successfully.");
//
//            log.trace("Fetching meeting for ID: {}", feedbackVO.getMeetingId());
//            Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(feedbackVO.getMeetingId()));
//            if (optionalMeeting.isEmpty()) {
//                log.error("ALERT_FOR_ERROR: Meeting not found for ID: {}", feedbackVO.getMeetingId());
//                throw new MatchmakingException("Meeting does not exist", DATA_NOT_FOUND);
//            }
//
//            log.trace("Fetching user for ID: {}", feedbackVO.getUserId());
//            Optional<BaseUserProfile> optionalUser = baseUserProfileRepository.findById(Integer.valueOf(feedbackVO.getUserId()));
//            if (optionalUser.isEmpty()) {
//                log.error("ALERT_FOR_ERROR: User not found for ID: {}", feedbackVO.getUserId());
//                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
//            }
//
//            log.trace("Checking for duplicate feedback for meeting ID: {} and user ID: {}", feedbackVO.getMeetingId(), feedbackVO.getUserId());
//            if (meetingFeedbackRepository.existsByMeetingIdAndCognitoSub(feedbackVO.getMeetingId(), optionalUser.get().getCognitoSub())) {
//                log.error("ALERT_FOR_ERROR: Feedback already submitted for meeting ID: {} by user ID: {}", feedbackVO.getMeetingId(), feedbackVO.getUserId());
//                throw new MatchmakingException("Feedback already submitted for this meeting by this user", DUPLICATE_RECORD);
//            }
//
//            log.trace("Saving meeting feedback record.");
//            MeetingFeedback feedback = feedbackVO.fromVO();
////            feedback.setMeeting(optionalMeeting.get());
////            feedback.setUser(optionalUser.get());
//            feedback = meetingFeedbackRepository.save(feedback);
//            log.info("Meeting feedback saved successfully with ID: {}", feedback.getId());
//
//            log.info("Triggering feedback decision engine for meeting ID: {}", feedbackVO.getMeetingId());
////            feedbackDecisionEngine.evaluate(feedbackVO.getMeetingId());
//
//            return new BaseVO(SUCCESS, "Feedback record saved", "Meeting feedback saved", new MeetingFeedbackVO().toVO(feedback));
//        } catch (MatchmakingException ex) {
//            throw ex;
//        } catch (Exception ex) {
//            log.error("ALERT_FOR_ERROR: Error occurred while adding feedback. Error: {}", ex.getMessage(), ex);
//            throw new MatchmakingException("Error occurred while adding feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
//        }
//    }
//
//    @Override
//    public BaseVO get(String id) throws MatchmakingException {
//        try {
//            log.info("Fetching feedback for ID: {}", id);
//            Optional<MeetingFeedback> optionalFeedback = meetingFeedbackRepository.findById(Integer.valueOf(id));
//            if (optionalFeedback.isEmpty()) {
//                log.error("ALERT_FOR_ERROR: Feedback not found for ID: {}", id);
//                throw new MatchmakingException("Feedback does not exist", DATA_NOT_FOUND);
//            }
//
//            log.info("Feedback found for ID: {}", id);
//            return new BaseVO(SUCCESS, "Feedback record fetched", "Feedback record fetched", new MeetingFeedbackVO().toVO(optionalFeedback.get()));
//        } catch (MatchmakingException ex) {
//            throw ex;
//        } catch (Exception ex) {
//            log.error("ALERT_FOR_ERROR: Error occurred while fetching feedback. Error: {}", ex.getMessage(), ex);
//            throw new MatchmakingException("Error occurred while fetching feedback: " + ex.getMessage(), UNKNOWN_EXCEPTION);
//        }
//    }
//
//    @Override
//    public BaseVO getAllForMeeting(String meetingId) throws MatchmakingException {
//        try {
//            log.info("Fetching all feedbacks for meeting ID: {}", meetingId);
//            List<MeetingFeedback> feedbacks = meetingFeedbackRepository.findByMeetingId(meetingId);
//            if (feedbacks.isEmpty()) {
//                log.error("ALERT_FOR_ERROR: No feedbacks found for meeting ID: {}", meetingId);
//                throw new MatchmakingException("No feedback found for the given meeting", DATA_NOT_FOUND);
//            }
//
//            List<MeetingFeedbackVO> result = feedbacks.stream()
//                    .map(f -> new MeetingFeedbackVO().toVO(f))
//                    .collect(Collectors.toList());
//
//            log.info("Fetched {} feedbacks for meeting ID: {}", result.size(), meetingId);
//            return new BaseVO(SUCCESS, "Feedbacks fetched", "Feedback records fetched for meeting", result);
//        } catch (MatchmakingException ex) {
//            throw ex;
//        } catch (Exception ex) {
//            log.error("ALERT_FOR_ERROR: Error occurred while fetching feedbacks for meeting. Error: {}", ex.getMessage(), ex);
//            throw new MatchmakingException("Error occurred while fetching feedbacks: " + ex.getMessage(), UNKNOWN_EXCEPTION);
//        }
//    }
//}
