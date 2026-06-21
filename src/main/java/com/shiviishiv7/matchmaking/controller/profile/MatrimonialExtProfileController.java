package com.shiviishiv7.matchmaking.controller.profile;



import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IMatrimonialExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatrimonialExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matrimonial-profile")
@Slf4j
@Tag(name = "Matrimonial Extended Profile", description = "Manages detailed traditional metrics including socio-cultural background, family structures, income parameters, and horoscopes")
public class MatrimonialExtProfileController {

    @Autowired
    private IMatrimonialExtProfileProcessor matrimonialProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended high-intent matrimonial biodata track node for a user")
    public ResponseEntity<BaseVO> add(@RequestBody MatrimonialExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add matrimonial profile for userId: {} by sub context: {}", vo.getCognitoSubB(), sub);

        BaseVO response = matrimonialProfileProcessor.add(vo);
        log.info("Successfully established extended matrimonial properties under sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update structural marriage configurations, community values, family details, or birth variables")
    public ResponseEntity<BaseVO> update(@RequestBody MatrimonialExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update matrimonial profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = matrimonialProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed matrimonial tracking metrics for instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Fetch a complete structural matrimonial profile by its tracking instance key")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch matrimonial profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = matrimonialProfileProcessor.getById(id);
        log.info("Successfully resolved structural matrimonial dataset for target key: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Retrieve a user's deep matrimonial profile layer assignment using their primary account registration ID")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query matrimonial profile extension block for userId: {} by sub context: {}", userId, sub);

        BaseVO response = matrimonialProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active matrimonial extension layer sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard delete a deep matrimonial profile track block assignment allocation")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete matrimonial extension record key: {} by sub context: {}", id, sub);

        BaseVO response = matrimonialProfileProcessor.delete(id);
        log.info("Successfully dropped extended matrimonial tracking tables entry matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
