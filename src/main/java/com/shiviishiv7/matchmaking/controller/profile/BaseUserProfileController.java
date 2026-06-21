package com.shiviishiv7.matchmaking.controller.profile;

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
@RequestMapping("/user")
@Slf4j
@Tag(name = "Base User Profile", description = "Core demographics, verification status, and profile tracking management")
public class BaseUserProfileController {

    @Autowired
    private IBaseUserProfileProcessor userProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Register base baseline profile info details")
    public ResponseEntity<BaseVO> add(@RequestBody BaseUserProfileVO userVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to save profile baseline state for email: {} from sub context: {}", userVO.getCognitoSub(), sub);

        // Context enforcement lock: Bind incoming payload lifecycle ownership to authenticated session sub identifier
        userVO.setCognitoSub(sub);

        BaseVO response = userProcessor.add(userVO);
        log.info("Successfully established baseline user tracking records for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal user record demographics")
    public ResponseEntity<BaseVO> update(@RequestBody BaseUserProfileVO userVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to modify profile data parameters for user reference ID: {} from sub context: {}", userVO.getId(), sub);

        // Defend endpoint against payload impersonation strategies
        userVO.setCognitoSub(sub);
        userVO.validate();

        BaseVO response = userProcessor.update(userVO);
        log.info("Successfully refreshed profile demographic variables for user tracking ID: {}", userVO.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve user baseline parameters via database primary tracking key")
    public ResponseEntity<BaseVO> get(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request processed to view profile details for record instance ID: {} from sub context: {}", id, sub);

        BaseVO response = userProcessor.get(id);
        log.info("Successfully resolved user entity mapping configuration metadata for tracking target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/by-email/{email}", produces = "application/json")
    @Operation(summary = "Lookup a user instance by primary communication address link")
    public ResponseEntity<BaseVO> getByEmail(@PathVariable String email) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request initiated to query user details matching address parameter: {} from sub context: {}", email, sub);

        BaseVO response = userProcessor.getByEmail(email);
        log.info("Successfully matched account registration details to target identity handle: {}", email);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Deactivate profile tracking structures")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to delete profile matching tracking reference index: {} from sub context: {}", id, sub);

        BaseVO response = userProcessor.delete(id);
        log.info("Successfully purged configuration assets mapping to user index identifier: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}