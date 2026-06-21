package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.meeting.IMeetingProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@Slf4j
public class MeetingController {

    @Autowired
    private IMeetingProcessor meetingProcessor;

    @PostMapping
    public ResponseEntity<BaseVO> add(@RequestBody MeetingVO vo) throws MatchmakingException {
        return ResponseEntity.ok(meetingProcessor.add(vo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseVO> get(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(meetingProcessor.get(id));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<BaseVO> getByMatch(@PathVariable String matchId) throws MatchmakingException {
        return ResponseEntity.ok(meetingProcessor.getByMatchId(matchId));
    }

    @GetMapping("/user/{cognitoSub}/upcoming")
    public ResponseEntity<BaseVO> getUpcoming(@PathVariable String cognitoSub) throws MatchmakingException {
        return ResponseEntity.ok(meetingProcessor.getUpcomingForUser(cognitoSub));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BaseVO> cancel(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(meetingProcessor.cancel(id));
    }
}
