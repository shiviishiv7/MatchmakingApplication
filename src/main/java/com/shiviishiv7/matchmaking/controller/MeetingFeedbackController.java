package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.processor.feedback.IMeetingFeedbackProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingFeedbackController {

    private final IMeetingFeedbackProcessor feedbackProcessor;

    /**
     * Submit post-meeting feedback.
     * Body: { "meetingId": "42", "response": "YES" | "NO", "notes": "optional" }
     * cognitoSub is always taken from the JWT — never sent by the client.
     */
    @PostMapping("/{meetingId}/feedback")
    public ResponseEntity<BaseVO> submit(@PathVariable String meetingId,
                                         @RequestBody MeetingFeedbackVO feedbackVO) throws MatchmakingException {
        feedbackVO.setMeetingId(meetingId);
        feedbackVO.setCognitoSub(CurrentUserContext.getCurrentUser().getUsername());
        log.info("Feedback submission: meetingId={} user={} response={}",
                meetingId, feedbackVO.getCognitoSub(), feedbackVO.getResponse());
        return ResponseEntity.ok(feedbackProcessor.submit(feedbackVO));
    }

    @GetMapping("/{meetingId}/feedback")
    public ResponseEntity<BaseVO> getAllForMeeting(@PathVariable String meetingId) throws MatchmakingException {
        return ResponseEntity.ok(feedbackProcessor.getAllForMeeting(meetingId));
    }
}
