package com.shiviishiv7.matchmaking.provider.vo.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries WebRTC signaling messages between two peers via the Spring STOMP broker.
 * The server never inspects the payload — it only routes the message to the target user.
 *
 * type values:
 *   OFFER         — SDP offer from caller
 *   ANSWER        — SDP answer from callee
 *   ICE_CANDIDATE — ICE candidate from either peer
 *   PEER_LEFT     — peer ended the call
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebRTCSignalVO {

    private String type;        // OFFER | ANSWER | ICE_CANDIDATE | PEER_LEFT
    private String fromUserId;
    private String toUserId;
    private String matchId;
    private String payload;     // raw SDP string or ICE candidate JSON
}
