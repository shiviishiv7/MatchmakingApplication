package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.baseuserprofile.IBaseUserProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseUserProfileVO;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/base-user-profile")
@Slf4j
@Tag(name = "Base User Profile", description = "Base user profile management operations")
public class BaseUserProfileController {

    @Autowired
    private IBaseUserProfileProcessor baseUserProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Add a new base user profile")
    public ResponseEntity<BaseVO> add(@RequestBody BaseUserProfileVO profileVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to add base profile for sub: {}", sub);

        // Ensure the Cognito sub from token is tied to the VO if needed
        if (profileVO.getCognitoSub() == null) {
            profileVO.setCognitoSub(sub);
        }

        BaseVO response = baseUserProfileProcessor.add(profileVO);
        log.info("Successfully added base profile for sub: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update an existing base user profile")
    public ResponseEntity<BaseVO> update(@RequestBody BaseUserProfileVO profileVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        // Always bind the authenticated user's sub — prevents updating another user's profile
        profileVO.setCognitoSub(sub);
        log.info("Request received to update base profile for sub: {}", sub);

        BaseVO response = baseUserProfileProcessor.update(profileVO);
        log.info("Successfully updated base profile for sub: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{cognitoSub}", produces = "application/json")
    @Operation(summary = "Get base user profile by Cognito sub")
    public ResponseEntity<BaseVO> getByCognitoSub(@PathVariable("cognitoSub") String cognitoSub) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch base profile for cognitoSub: {} by sub: {}", cognitoSub, sub);

        BaseVO response = baseUserProfileProcessor.getByCognitoSub(cognitoSub);
        log.info("Successfully fetched base profile for cognitoSub: {} by sub: {}", cognitoSub, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/by-userid/{userId}", produces = "application/json")
    @Operation(summary = "Get base user profile by Cognito user ID")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch base profile for userId: {} by sub: {}", userId, sub);

        BaseVO response = baseUserProfileProcessor.getByUserId(userId);
        log.info("Successfully fetched base profile for userId: {} by sub: {}", userId, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/by-email/{email}", produces = "application/json")
    @Operation(summary = "Get base user profile by email")
    public ResponseEntity<BaseVO> getByEmail(@PathVariable String email) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch base profile with email: {} by sub: {}", email, sub);

        BaseVO response = baseUserProfileProcessor.getByEmail(email);
        log.info("Successfully fetched base profile with email: {} by sub: {}", email, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/delete/{cognitoSub}", produces = "application/json")
    @Operation(summary = "Soft-delete/Deactivate base user profile by Cognito sub")
    public ResponseEntity<BaseVO> delete(@PathVariable("cognitoSub") String cognitoSub) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to deactivate base profile for cognitoSub: {} by sub: {}", cognitoSub, sub);

        BaseVO response = baseUserProfileProcessor.delete(cognitoSub);
        log.info("Successfully deactivated base profile for cognitoSub: {} by sub: {}", cognitoSub, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}