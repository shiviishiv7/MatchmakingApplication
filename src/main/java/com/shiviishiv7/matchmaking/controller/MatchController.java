package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.util.security.CurrentUserContext;
import com.shiviishiv7.matchmaking.processor.match.IMatchProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.service.match.MatchConnectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
@Slf4j
public class MatchController {

    @Autowired private IMatchProcessor matchProcessor;
    @Autowired private MatchConnectService matchConnectService;

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

    /**
     * Called by the client after a call ends (or on app open) to request the next match.
     * Scans PENDING MatchResults for the authenticated user and connects the first online candidate.
     * Returns 200 with connecting=true if a call was initiated, connecting=false if no one is online.
     */
    @PostMapping("/next")
    public ResponseEntity<BaseVO> next() {
        String cognitoSub = CurrentUserContext.getCurrentUser().getUsername();
        boolean connected = matchConnectService.connectNextOnlineMatch(cognitoSub);
        if (connected) {
            return ResponseEntity.ok(
                    new BaseVO(200, "Connecting now", "Match found and call started. Check your call screen.", null));
        } else {
            return ResponseEntity.ok(
                    new BaseVO(200, "No active match", "No match is online right now. We'll notify you when one becomes available.", null));
        }
    }
}
