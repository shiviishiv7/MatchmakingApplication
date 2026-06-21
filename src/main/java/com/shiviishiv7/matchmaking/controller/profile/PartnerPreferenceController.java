package com.shiviishiv7.matchmaking.controller.profile;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IPartnerPreferenceProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/partner-preference")
@Slf4j
@Tag(name = "Partner Preference Profile", description = "Manages ideal criteria matrices for partner discovery engines (age ranges, location filters, socio-cultural metrics)")
public class PartnerPreferenceController {

    @Autowired
    private IPartnerPreferenceProcessor partnerPreferenceProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Establish a new ideal partner preference configuration block")
    public ResponseEntity<BaseVO> add(@RequestBody PartnerPreferenceVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add partner preferences for cognitoSub: {} by sub context: {}", vo.getCognitoSub(), sub);

        BaseVO response = partnerPreferenceProcessor.add(vo);
        log.info("Successfully established partner criteria matrices under sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Refine age, height, income bounds, or socio-cultural partner requirement settings")
    public ResponseEntity<BaseVO> update(@RequestBody PartnerPreferenceVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update partner preferences template ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = partnerPreferenceProcessor.update(vo);
        log.info("Successfully refreshed detailed filter variables for criteria block reference ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Retrieve a partner preference template by its tracking node ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch partner preferences profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = partnerPreferenceProcessor.getById(id);
        log.info("Successfully resolved structural preference configurations for target node key: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Fetch partner filters linked to a user profile using their security context identifier")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query partner criteria requirements mapped to context identifier: {} by sub context: {}", userId, sub);

        BaseVO response = partnerPreferenceProcessor.getByUserId(userId);
        log.info("Successfully mapped active search constraints matrix for context sub reference: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard delete a partner preference structural restriction profile")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete partner preference rule card key: {} by sub context: {}", id, sub);

        BaseVO response = partnerPreferenceProcessor.delete(id);
        log.info("Successfully dropped structural preference layers matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
