package com.shiviishiv7.matchmaking.controller;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IGamingExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.GamingExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gaming-profile")
@Slf4j
@Tag(name = "Gaming Extended Profile", description = "Manages specialized gaming preferences, platform configurations, and squad matching metrics")
public class GamingExtProfileController {

    @Autowired
    private IGamingExtProfileProcessor gamingProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended gaming and squad profile for a user")
    public ResponseEntity<BaseVO> add(@RequestBody GamingExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add gaming profile for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = gamingProfileProcessor.add(vo);
        log.info("Successfully established extended gaming parameters for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal gaming platforms, favorite titles, skill levels, or gamertags")
    public ResponseEntity<BaseVO> update(@RequestBody GamingExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update gaming profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = gamingProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed gaming variables for profile reference instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve a gaming profile configuration via its unique primary tracking key ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch gaming profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = gamingProfileProcessor.getById(id);
        log.info("Successfully resolved gaming profile details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Fetch the extended gaming profile data linked to a core user record identifier")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query gaming extended layer parameters for userId: {} by sub context: {}", userId, sub);

        BaseVO response = gamingProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active gaming extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a gaming profile structural block record assignment")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete gaming extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = gamingProfileProcessor.delete(id);
        log.info("Successfully dropped extended gaming records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
