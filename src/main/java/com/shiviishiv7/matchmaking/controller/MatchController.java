package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.match.IMatchProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
@Slf4j
public class MatchController {

    @Autowired
    private IMatchProcessor matchProcessor;

    @GetMapping("/{id}")
    public ResponseEntity<BaseVO> get(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(matchProcessor.get(id));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<BaseVO> getActiveForUser(@PathVariable String userId) throws MatchmakingException {
        return ResponseEntity.ok(matchProcessor.getActiveMatchForUser(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<BaseVO> getByStatus(@PathVariable String status) throws MatchmakingException {
        return ResponseEntity.ok(matchProcessor.getAllByStatus(status));
    }

    @PatchMapping("/{id}/end")
    public ResponseEntity<BaseVO> end(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(matchProcessor.end(id));
    }
}
