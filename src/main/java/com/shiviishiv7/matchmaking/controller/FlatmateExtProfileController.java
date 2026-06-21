package com.shiviishiv7.matchmaking.controller;



import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IFlatmateExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.FlatmateExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flatmate-profile")
@Slf4j
@Tag(name = "Flatmate Extended Profile", description = "Manages specialized living preferences, budgets, and rooming habits parameters")
public class FlatmateExtProfileController {

    @Autowired
    private IFlatmateExtProfileProcessor flatmateProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended flatmate or rooming profile for a user")
    public ResponseEntity<BaseVO> add(@RequestBody FlatmateExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add flatmate profile for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = flatmateProfileProcessor.add(vo);
        log.info("Successfully established extended flatmate parameters for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal living schedules, cleanliness standards, or guest policies")
    public ResponseEntity<BaseVO> update(@RequestBody FlatmateExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update flatmate profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = flatmateProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed flatmate variables for profile reference instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve a flatmate configuration card via its unique primary tracking key ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch flatmate profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = flatmateProfileProcessor.getById(id);
        log.info("Successfully resolved flatmate profile details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Fetch the extended flatmate profile data linked to a core user record identifier")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query flatmate extended layer parameters for userId: {} by sub context: {}", userId, sub);

        BaseVO response = flatmateProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active flatmate extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a flatmate profile structural block record assignment")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete flatmate extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = flatmateProfileProcessor.delete(id);
        log.info("Successfully dropped extended flatmate records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
