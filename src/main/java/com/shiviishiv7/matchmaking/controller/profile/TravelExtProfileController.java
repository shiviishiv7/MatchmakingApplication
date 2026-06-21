package com.shiviishiv7.matchmaking.controller.profile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.ITravelExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.TravelExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/travel-profile")
@Slf4j
@Tag(name = "Travel Extended Profile", description = "Manages specialized travel styles, destination bucket lists, budget tolerances, and co-traveler matching variables")
public class TravelExtProfileController {

    @Autowired
    private ITravelExtProfileProcessor travelProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended travel preference profile for a user")
    public ResponseEntity<BaseVO> add(@RequestBody TravelExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add travel profile for userId: {} by sub context: {}", vo.getCognitoSub(), sub);

        BaseVO response = travelProfileProcessor.add(vo);
        log.info("Successfully established extended travel matching metrics under sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal trip itineraries, budget stay permissions, or upcoming travel details")
    public ResponseEntity<BaseVO> update(@RequestBody TravelExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update travel profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = travelProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed travel variables for profile instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Fetch a travel preferences node via its unique primary key tracking ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch travel profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = travelProfileProcessor.getById(id);
        log.info("Successfully resolved adventure profile details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Retrieve a user's travel extension mapping dataset using their primary account registration ID")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query travel extended parameters block for userId: {} by sub context: {}", userId, sub);

        BaseVO response = travelProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active travel extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a travel extension tracker record assignment block")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete travel extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = travelProfileProcessor.delete(id);
        log.info("Successfully dropped extended travel records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}