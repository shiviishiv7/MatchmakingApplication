package com.shiviishiv7.matchmaking.controller;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IDatingExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.DatingExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dating-profile")
@Slf4j
@Tag(name = "Dating Extended Profile", description = "Manages specialized lifestyle, preference, and high-intent dating parameters")
public class DatingExtProfileController {

    @Autowired
    private IDatingExtProfileProcessor datingProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended dating profile for a user")
    public ResponseEntity<BaseVO> add(@RequestBody DatingExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add dating profile for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = datingProfileProcessor.add(vo);
        log.info("Successfully established extended dating profile parameters for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal dating metrics, prompts, or match preferences")
    public ResponseEntity<BaseVO> update(@RequestBody DatingExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update dating profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = datingProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed dating variables for profile reference instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve a dating profile configuration card via its unique tracking ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch dating profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = datingProfileProcessor.getById(id);
        log.info("Successfully resolved dating profile details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Fetch the extended dating profile node linked to a primary user record identifier")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query dating extended layer parameters for userId: {} by sub context: {}", userId, sub);

        BaseVO response = datingProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active dating extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a dating profile configuration asset mapping block")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete dating extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = datingProfileProcessor.delete(id);
        log.info("Successfully dropped extended dating records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
