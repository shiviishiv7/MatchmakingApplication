package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.feedback.IMeetingFeedbackProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/meeting-feedback")
@Slf4j
@Tag(name = "Meeting Feedback", description = "Post-meeting feedback submission and retrieval")
public class MeetingFeedbackController {

    @Autowired
    private IMeetingFeedbackProcessor meetingFeedbackProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody MeetingFeedbackVO feedbackVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to submit feedback for meeting ID: {} by user ID: {} sub: {}", feedbackVO.getMeetingId(), feedbackVO.getUserId(), sub);

        BaseVO response = meetingFeedbackProcessor.add(feedbackVO);
        log.info("Successfully submitted feedback for meeting ID: {} by user ID: {} sub: {}", feedbackVO.getMeetingId(), feedbackVO.getUserId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> get(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch feedback with ID: {} by sub: {}", id, sub);

        BaseVO response = meetingFeedbackProcessor.get(id);
        log.info("Successfully fetched feedback with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/meeting/{meetingId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getAllForMeeting(@PathVariable("meetingId") String meetingId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch all feedbacks for meeting ID: {} by sub: {}", meetingId, sub);

        BaseVO response = meetingFeedbackProcessor.getAllForMeeting(meetingId);
        log.info("Successfully fetched all feedbacks for meeting ID: {} by sub: {}", meetingId, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
