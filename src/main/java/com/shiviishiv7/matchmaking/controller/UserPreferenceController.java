package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.preference.IUserPreferenceProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user-preference")
@Slf4j
@Tag(name = "User Preference", description = "Match preference settings per user")
public class UserPreferenceController {

    @Autowired
    private IUserPreferenceProcessor userPreferenceProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody UserPreferenceVO preferenceVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to add preference for user ID: {} by sub: {}", preferenceVO.getUserId(), sub);

        BaseVO response = userPreferenceProcessor.add(preferenceVO);
        log.info("Successfully added preference for user ID: {} by sub: {}", preferenceVO.getUserId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> update(@RequestBody UserPreferenceVO preferenceVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to update preference with ID: {} by sub: {}", preferenceVO.getId(), sub);

        BaseVO response = userPreferenceProcessor.update(preferenceVO);
        log.info("Successfully updated preference with ID: {} by sub: {}", preferenceVO.getId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") UUID userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch preference for user ID: {} by sub: {}", userId, sub);

        BaseVO response = userPreferenceProcessor.getByUserId(userId);
        log.info("Successfully fetched preference for user ID: {} by sub: {}", userId, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
