package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.user.IUserProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "User", description = "User registration and profile management")
public class UserController {

    @Autowired
    private IUserProcessor userProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody UserVO userVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to add user with email: {} by sub: {}", userVO.getEmail(), sub);

        BaseVO response = userProcessor.add(userVO);
        log.info("Successfully added user with email: {} by sub: {}", userVO.getEmail(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> update(@RequestBody UserVO userVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to update user with ID: {} by sub: {}", userVO.getId(), sub);

        BaseVO response = userProcessor.update(userVO);
        log.info("Successfully updated user with ID: {} by sub: {}", userVO.getId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> get(@PathVariable("id") UUID id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch user with ID: {} by sub: {}", id, sub);

        BaseVO response = userProcessor.get(id);
        log.info("Successfully fetched user with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/by-email", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getByEmail(@RequestParam(required = true) String email) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch user with email: {} by sub: {}", email, sub);

        BaseVO response = userProcessor.getByEmail(email);
        log.info("Successfully fetched user with email: {} by sub: {}", email, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> delete(@PathVariable("id") UUID id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to delete user with ID: {} by sub: {}", id, sub);

        BaseVO response = userProcessor.delete(id);
        log.info("Successfully deleted user with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
