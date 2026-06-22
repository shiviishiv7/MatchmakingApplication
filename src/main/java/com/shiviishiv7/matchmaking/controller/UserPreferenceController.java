package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.preference.IUserPreferenceProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-preference")
@Slf4j
@Tag(name = "User Preference", description = "Match filter settings per user per category")
public class UserPreferenceController {

    @Autowired
    private IUserPreferenceProcessor userPreferenceProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/save", produces = "application/json", consumes = "application/json")
    public ResponseEntity<BaseVO> save(@RequestBody MatchFilterVO filterVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        filterVO.setCognitoSub(sub);
        log.info("Save match filter request for cognitoSub: {}", sub);
        BaseVO response = userPreferenceProcessor.save(filterVO);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{cognitoSub}", produces = "application/json")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("cognitoSub") String cognitoSub) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Fetch match filters request for cognitoSub: {} by sub: {}", cognitoSub, sub);
        BaseVO response = userPreferenceProcessor.getByUserId(cognitoSub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
