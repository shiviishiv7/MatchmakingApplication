package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.categoryprofileregistry.ICategoryProfileRegistryProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/match-filter")
@Slf4j
@Tag(name = "Match Filter", description = "Save and retrieve a user's match preferences for a given category")
public class MatchFilterController {

    @Autowired
    private ICategoryProfileRegistryProcessor categoryProfileRegistryProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    /**
     * Save (or upsert) the authenticated user's match filter for a category.
     * cognitoSub is always taken from the JWT — the client never needs to send it.
     */
    @PostMapping("/save")
    @Operation(summary = "Save match filter preferences for the current user")
    public ResponseEntity<BaseVO> save(@RequestBody MatchFilterVO vo) throws MatchmakingException {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        vo.setCognitoSub(cognitoSub);
        log.info("Saving match filter for cognitoSub: {} category: {}", cognitoSub, vo.getChildCategory());
        return ResponseEntity.ok(categoryProfileRegistryProcessor.add(vo));
    }

    /**
     * Get all active match filter registrations for the authenticated user.
     */
    @GetMapping("/my")
    @Operation(summary = "Get all active match filters for the current user")
    public ResponseEntity<BaseVO> getMy() throws MatchmakingException {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        log.info("Fetching active match filters for cognitoSub: {}", cognitoSub);
        return ResponseEntity.ok(categoryProfileRegistryProcessor.getActiveByUserId(cognitoSub));
    }

    /**
     * Deactivate a specific category filter for the authenticated user.
     */
    @DeleteMapping("/{matchCategory}")
    @Operation(summary = "Deactivate a match filter for a specific category")
    public ResponseEntity<BaseVO> deactivate(@PathVariable String matchCategory) throws MatchmakingException {
        String cognitoSub = securityUtility.getAuthenticatedUserSub();
        log.info("Deactivating match filter for cognitoSub: {} category: {}", cognitoSub, matchCategory);
        return ResponseEntity.ok(categoryProfileRegistryProcessor.deactivate(cognitoSub, matchCategory));
    }
}
