package com.shiviishiv7.matchmaking.processor.other_data;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.feedback.IMeetingFeedbackProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingFeedbackVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@Slf4j
public class MeetingFeedbackController {

    @Autowired
    private IMeetingFeedbackProcessor feedbackProcessor;

    @PostMapping
    public ResponseEntity<BaseVO> submit(@RequestBody MeetingFeedbackVO vo) throws MatchmakingException {
        return ResponseEntity.ok(feedbackProcessor.submit(vo));
    }

    @GetMapping("/meeting/{meetingId}")
    public ResponseEntity<BaseVO> getByMeeting(@PathVariable String meetingId) throws MatchmakingException {
        return ResponseEntity.ok(feedbackProcessor.getByMeeting(meetingId));
    }
}
