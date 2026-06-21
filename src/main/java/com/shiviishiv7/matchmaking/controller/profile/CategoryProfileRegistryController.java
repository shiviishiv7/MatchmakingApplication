package com.shiviishiv7.matchmaking.controller.profile;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.categoryprofileregistry.ICategoryProfileRegistryProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category-registry")
@Slf4j
@Tag(name = "Category Profile Registry", description = "Manages individual user intent category mappings (Matrimony, Exam Prep, etc.)")
public class CategoryProfileRegistryController {

    @Autowired
    private ICategoryProfileRegistryProcessor registryProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(value = "/add", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Register a user under a specific matching intent track")
    public ResponseEntity<BaseVO> add(@RequestBody CategoryProfileRegistryVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to add category profile mapping for userId: {} by sub context: {}", vo.getUserId(), sub);

        BaseVO response = registryProcessor.add(vo);
        log.info("Successfully registered intent category tracking record for sub context: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/update", produces = "application/json", consumes = "application/json")
    @Operation(summary = "Update progress metric values or state flags for an intent registration")
    public ResponseEntity<BaseVO> update(@RequestBody CategoryProfileRegistryVO vo) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to update category profile mapping ID: {} by sub context: {}", vo.getId(), sub);

        BaseVO response = registryProcessor.update(vo);
        log.info("Successfully applied parameters update on registry entry reference ID: {}", vo.getId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Fetch a single category registration instance by tracking ID")
    public ResponseEntity<BaseVO> getById(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch profile registry instance ID: {} by sub context: {}", id, sub);

        BaseVO response = registryProcessor.getById(id);
        log.info("Successfully fetched registration tracking attributes for instance: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "Get all category intent registration nodes matching a user tracking target")
    public ResponseEntity<BaseVO> getAllByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch all category mapping elements for user identifier: {} by sub context: {}", userId, sub);

        BaseVO response = registryProcessor.getAllByUserId(userId);
        log.info("Successfully extracted category registration history collection for target: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userId}/active", produces = "application/json")
    @Operation(summary = "Fetch running/active matching track pools for an individual user profile")
    public ResponseEntity<BaseVO> getActiveByUserId(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to fetch active search filters for user baseline profile: {} by sub context: {}", userId, sub);

        BaseVO response = registryProcessor.getActiveByUserId(userId);
        log.info("Successfully mapped active pool intents collection for user reference target: {}", userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/deactivate", produces = "application/json")
    @Operation(summary = "Deactivate a specific category track for a user without wiping its tracking configuration")
    public ResponseEntity<BaseVO> deactivate(
            @RequestParam("userId") String userId,
            @RequestParam("matchCategory") String matchCategory) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to flip active bit flag to false for user: {} category track: {} by sub context: {}", userId, matchCategory, sub);

        BaseVO response = registryProcessor.deactivate(userId, matchCategory);
        log.info("Successfully paused matching cycles for track context profile mapping configuration.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/{id}", produces = "application/json")
    @Operation(summary = "Hard purge an intent registry track block assignment record")
    public ResponseEntity<BaseVO> delete(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("REST request to hard purge intent tracking node key: {} by sub context: {}", id, sub);

        BaseVO response = registryProcessor.delete(id);
        log.info("Successfully dropped registry tracking entry allocation layer for target reference: {}", id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
