package com.shiviishiv7.matchmaking.controller;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.userprofile.IProfessionalExtProfileProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.ProfessionalExtProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/professional-profile")
@Slf4j
@Tag(name = "Professional Extended Profile", description = "Manages corporate backgrounds, technical stacks, mentorship settings, and startup collaboration parameters")
public class ProfessionalExtProfileController {

    @Autowired
    private IProfessionalExtProfileProcessor professionalProfileProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Create an extended professional networking profile card for a user")
    public ResponseEntity<BaseVO> add(@RequestBody ProfessionalExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add professional profile for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = professionalProfileProcessor.add(vo);
        log.info("Successfully established extended professional networking parameters under sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update personal corporate roles, tech stacks, mentorship preferences, or social portfolio handles")
    public ResponseEntity<BaseVO> update(@RequestBody ProfessionalExtProfileVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update professional profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = professionalProfileProcessor.update(vo);
        log.info("Successfully refreshed detailed professional variables for profile instance ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Fetch a professional profile structure via its primary key identifier")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch professional profile metadata instance ID: {} by sub context: {}", id, sub);

        BaseVO response = professionalProfileProcessor.getById(id);
        log.info("Successfully resolved corporate portfolio details for record node target: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Retrieve a user's professional extension mapping layer using their core account registration ID")
    public ResponseEntity<BaseVO> getByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to query professional extended parameters block for userId: {} by sub context: {}", userId, sub);

        BaseVO response = professionalProfileProcessor.getByUserId(userId);
        log.info("Successfully mapped active professional extension sub-record for user: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge a professional extension tracker record assignment block")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard delete professional extension layer key: {} by sub context: {}", id, sub);

        BaseVO response = professionalProfileProcessor.delete(id);
        log.info("Successfully dropped extended professional records matching key index: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
