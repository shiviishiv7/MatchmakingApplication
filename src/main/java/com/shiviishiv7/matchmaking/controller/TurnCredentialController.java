package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Serves TURN server credentials to authenticated clients so the frontend
 * can configure WebRTC PeerConnection with ICE servers.
 *
 * The client passes these directly into RTCPeerConnection({ iceServers: [...] }).
 */
@RestController
@RequestMapping("/webrtc")
@Slf4j
@Tag(name = "WebRTC", description = "TURN/STUN ICE server credentials for WebRTC peer connections")
public class TurnCredentialController {

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @Value("${webrtc.turn.url}")
    private String turnUrl;

    @Value("${webrtc.turn.username}")
    private String turnUsername;

    @Value("${webrtc.turn.credential}")
    private String turnCredential;

    @Value("${webrtc.stun.url}")
    private String stunUrl;

    @RequestMapping(value = "/ice-servers", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity<BaseVO> getIceServers() throws MatchmakingException {
        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("ICE server credentials requested by sub: {}", sub);

        List<Map<String, Object>> iceServers = List.of(
                Map.of("urls", stunUrl),
                Map.of(
                        "urls",       turnUrl,
                        "username",   turnUsername,
                        "credential", turnCredential
                )
        );

        return new ResponseEntity<>(
                new BaseVO(200, "ICE servers fetched", "Use these in RTCPeerConnection iceServers config", iceServers),
                HttpStatus.OK
        );
    }
}
