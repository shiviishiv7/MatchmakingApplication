package com.shiviishiv7.matchmaking.provider.vo.ws;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries all WebRTC signaling messages between two peers via Spring STOMP broker.
 *
 * type values:
 *   JOIN              — user joined the pool, server sends back pool list
 *   CONNECTION_REQUEST — caller wants to connect to a specific user
 *   OFFER             — SDP offer from caller → forwarded to callee
 *   ANSWER            — SDP answer from callee → forwarded to caller
 *   ICE_CANDIDATE     — ICE candidate from either peer → forwarded to the other
 *   PEER_LEFT         — peer ended the call
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebRTCSignalVO {

    private String type;         // JOIN | CONNECTION_REQUEST | OFFER | ANSWER | ICE_CANDIDATE | PEER_LEFT
    private String fromUserId;   // Cognito sub of the sender
    private String toUserId;     // Cognito sub of the target user
    private String payload;      // SDP string or ICE candidate JSON
}
