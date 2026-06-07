package com.shiviishiv7.matchmaking.controller.ws;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.vo.ws.PoolUserVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.WebRTCSignalVO;
import com.shiviishiv7.matchmaking.service.pool.UserPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

/**
 * Handles WebRTC signaling over STOMP WebSocket.
 *
 * Full flow:
 *  1. /app/webrtc.join              — user joins pool, receives available users list
 *  2. /app/webrtc.request           — caller sends connection request to a specific user
 *  3. /app/webrtc.offer             — caller sends SDP offer → forwarded to callee
 *  4. /app/webrtc.answer            — callee sends SDP answer → forwarded to caller
 *  5. /app/webrtc.ice               — either peer sends ICE candidate → forwarded to other peer
 *  6. /app/webrtc.leave             — user leaves, pool updated, other peer notified
 *
 * All responses go to /user/{sub}/queue/webrtc on the client.
 */
@Controller
@Slf4j
public class MatchmakingWebSocketController {

    private static final String USER_QUEUE = "/queue/webrtc";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPoolService userPoolService;

    // ─── Step 1: User Joins Pool ──────────────────────────────────────────────

    /**
     * Called when a user opens the find-match screen.
     * Adds them to the pool and sends them the list of all other available users.
     */
    @MessageMapping("/webrtc.join")
    public void userJoin(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
        String sub = getPrincipalName(headerAccessor);
        log.info("[JOIN] User joined pool: {}", sub);

        // Resolve user from DB and add to pool
        User user = resolveUser(sub);
        userPoolService.addUser(new PoolUserVO(
                sub,
                user.getFirstName(),
                user.getLastName(),
                user.getIndustry()
        ));

        // Send the available user list back to the joining user only
        List<PoolUserVO> availableUsers = userPoolService.getOtherUsers(sub);
        WebRTCSignalVO response = new WebRTCSignalVO();
        response.setType("POOL_LIST");
        response.setFromUserId("server");
        response.setToUserId(sub);
        response.setPayload(availableUsers.toString()); // serialized by Spring as JSON

        messagingTemplate.convertAndSendToUser(sub, USER_QUEUE, availableUsers);
        log.info("[JOIN] Sent pool list ({} users) to {}", availableUsers.size(), sub);
    }

    // ─── Step 2: Caller sends Connection Request ──────────────────────────────

    /**
     * Caller selected a user and wants to connect.
     * Forward the request to the selected (callee) user.
     */
    @MessageMapping("/webrtc.request")
    public void connectionRequest(@Payload WebRTCSignalVO signal,
                                  SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        signal.setType("CONNECTION_REQUEST");

        log.info("[REQUEST] {} wants to connect to {}", callerSub, signal.getToUserId());

        // Forward request to callee
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 3: Caller sends SDP Offer ──────────────────────────────────────

    /**
     * Callee accepted the request.
     * Now the CALLER creates an SDP offer and sends it here.
     * Server forwards it to the callee.
     */
    @MessageMapping("/webrtc.offer")
    public void handleOffer(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        signal.setType("OFFER");

        log.info("[OFFER] Forwarding SDP offer from {} to {}", callerSub, signal.getToUserId());

        // Forward offer to callee
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 4: Callee sends SDP Answer ─────────────────────────────────────

    /**
     * Callee received the offer, created an answer.
     * Server forwards the SDP answer back to the caller.
     */
    @MessageMapping("/webrtc.answer")
    public void handleAnswer(@Payload WebRTCSignalVO signal,
                             SimpMessageHeaderAccessor headerAccessor) {
        String calleeSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(calleeSub);
        signal.setType("ANSWER");

        log.info("[ANSWER] Forwarding SDP answer from {} to {}", calleeSub, signal.getToUserId());

        // Forward answer to caller
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 5: ICE Candidate Exchange ──────────────────────────────────────

    /**
     * Either peer generated an ICE candidate.
     * Server forwards it to the other peer.
     * This happens continuously on both sides until P2P is established.
     */
    @MessageMapping("/webrtc.ice")
    public void handleIceCandidate(@Payload WebRTCSignalVO signal,
                                   SimpMessageHeaderAccessor headerAccessor) {
        String senderSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(senderSub);
        signal.setType("ICE_CANDIDATE");

        log.info("[ICE] Forwarding ICE candidate from {} to {}", senderSub, signal.getToUserId());

        // Forward ICE candidate to the other peer
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 6: User Leaves ─────────────────────────────────────────────────

    /**
     * User left the screen or ended the call.
     * Remove from pool and notify the other peer if they were in a call.
     */
    @MessageMapping("/webrtc.leave")
    public void handleLeave(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String leavingSub = getPrincipalName(headerAccessor);
        userPoolService.removeUser(leavingSub);
        log.info("[LEAVE] User {} left the pool", leavingSub);

        // Notify the other peer if they were in a call
        if (signal.getToUserId() != null) {
            WebRTCSignalVO leaveSignal = new WebRTCSignalVO();
            leaveSignal.setType("PEER_LEFT");
            leaveSignal.setFromUserId(leavingSub);
            leaveSignal.setToUserId(signal.getToUserId());

            messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, leaveSignal);
            log.info("[LEAVE] Notified {} that {} has left", signal.getToUserId(), leavingSub);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getPrincipalName(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() == null) {
            throw new IllegalStateException("No authenticated principal in WebSocket session.");
        }
        return headerAccessor.getUser().getName();
    }

    private User resolveUser(String cognitoSub) throws MatchmakingException {
        Optional<User> user = userRepository.findByCognitoSub(cognitoSub);
        if (user.isEmpty()) {
            throw new MatchmakingException("User not found for sub: " + cognitoSub, 401);
        }
        return user.get();
    }
}
