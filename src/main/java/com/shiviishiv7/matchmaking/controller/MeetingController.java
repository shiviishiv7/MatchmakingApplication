package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.processor.meeting.IMeetingProcessor;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MeetingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/meeting")
@Slf4j
@Tag(name = "Meeting", description = "Meeting scheduling and status management")
public class MeetingController {

    @Autowired
    private IMeetingProcessor meetingProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> add(@RequestBody MeetingVO meetingVO) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to schedule meeting for match ID: {} by sub: {}", meetingVO.getMatchId(), sub);

        BaseVO response = meetingProcessor.add(meetingVO);
        log.info("Successfully scheduled meeting for match ID: {} by sub: {}", meetingVO.getMatchId(), sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> get(@PathVariable("id") UUID id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch meeting with ID: {} by sub: {}", id, sub);

        BaseVO response = meetingProcessor.get(id);
        log.info("Successfully fetched meeting with ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/match/{matchId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getAllForMatch(@PathVariable("matchId") UUID matchId) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to fetch all meetings for match ID: {} by sub: {}", matchId, sub);

        BaseVO response = meetingProcessor.getAllForMatch(matchId);
        log.info("Successfully fetched all meetings for match ID: {} by sub: {}", matchId, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/complete/{id}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> markCompleted(@PathVariable("id") UUID id) throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Request received to mark meeting as completed for ID: {} by sub: {}", id, sub);

        BaseVO response = meetingProcessor.markCompleted(id);
        log.info("Successfully marked meeting as completed for ID: {} by sub: {}", id, sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
