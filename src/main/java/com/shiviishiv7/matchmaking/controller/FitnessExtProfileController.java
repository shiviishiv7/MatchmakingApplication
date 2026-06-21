package com.shiviishiv7.matchmaking.controller;



import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IFitnessExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.FitnessExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fitness-profile")
@Slf4j
@Tag(name = "Fitness Extended Profile", description = "Manages specialized fitness goals, activities, and sports matching metrics")
public class FitnessExtProfileController {

    @Autowired
    private IFitnessExtProfileProcessor fitnessProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended fitness and sports profile for a user")
    public ResponseEntity<BaseVO> add(@RequestBody FitnessExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add fitness profile for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = fitnessProfileProcessor.add(vo);
        log.info("Successfully established extended fitness parameters for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal workout habits, gym locations, or sports metrics")
    public ResponseEntity<BaseVO> update(@RequestBody FitnessExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update fitness profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = fitnessProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed fitness variables for profile reference instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve a fitness tracking configuration via its unique primary key ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch fitness profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = fitnessProfileProcessor.getById(id);
        log.info("Successfully resolved fitness profile details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Fetch the extended fitness profile data linked to a core user record identifier")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query fitness extended layer parameters for userId: {} by sub context: {}", userId, sub);

        BaseVO response = fitnessProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active fitness extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a fitness profile structural block record assignment")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete fitness extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = fitnessProfileProcessor.delete(id);
        log.info("Successfully dropped extended fitness records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
