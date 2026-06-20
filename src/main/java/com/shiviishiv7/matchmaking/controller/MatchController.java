package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.match.IMatchProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/match")
@Slf4j
@Tag(name = "Match", description = "Match creation and lifecycle management")
public class MatchController {

    @Autowired
    private IMatchProcessor matchProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody MatchVO matchVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to create match between userA: {} and userB: {} by sub: {}", matchVO.getCognitoSubA(), matchVO.getCognitoSubB(), sub);

        BaseVO response = matchProcessor.add(matchVO);
        log.info("Successfully created match between userA: {} and userB: {} by sub: {}", matchVO.getCognitoSubA(), matchVO.getCognitoSubB(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> get(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch match with ID: {} by sub: {}", id, sub);

        BaseVO response = matchProcessor.get(id);
        log.info("Successfully fetched match with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/active/user/{userId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getActiveMatchForUser(@PathVariable("userId") String userId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch active match for user ID: {} by sub: {}", userId, sub);

        BaseVO response = matchProcessor.getActiveMatchForUser(userId);
        log.info("Successfully fetched active match for user ID: {} by sub: {}", userId, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getAllByStatus(@RequestParam(required = true) String status) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch all matches with status: {} by sub: {}", status, sub);

        BaseVO response = matchProcessor.getAllByStatus(status);
        log.info("Successfully fetched all matches with status: {} by sub: {}", status, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/end/{id}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> end(@PathVariable("id") String id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to end match with ID: {} by sub: {}", id, sub);

        BaseVO response = matchProcessor.end(id);
        log.info("Successfully ended match with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
