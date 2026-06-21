package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.user.IUserProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    @Autowired
    private IUserProcessor userProcessor;

    @PostMapping
    public ResponseEntity<BaseVO> add(@RequestBody UserVO vo) throws MatchmakingException {
        return ResponseEntity.ok(userProcessor.add(vo));
    }

    @PutMapping
    public ResponseEntity<BaseVO> update(@RequestBody UserVO vo) throws MatchmakingException {
        return ResponseEntity.ok(userProcessor.update(vo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseVO> get(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(userProcessor.get(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<BaseVO> getByEmail(@PathVariable String email) throws MatchmakingException {
        return ResponseEntity.ok(userProcessor.getByEmail(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseVO> delete(@PathVariable String id) throws MatchmakingException {
        return ResponseEntity.ok(userProcessor.delete(id));
    }
}
