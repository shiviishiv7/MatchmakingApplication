package com.shiviishiv7.matchmaking.controller;


import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.processor.instant.IInstantMatchProcessor;
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
 *  1. /app/webrtc.join    — user joins pool + instant match search fires
 *  2. /app/webrtc.request — caller sends connection request to a specific user
 *  3. /app/webrtc.offer   — caller sends SDP offer → forwarded to callee
 *  4. /app/webrtc.answer  — callee sends SDP answer → forwarded to caller
 *  5. /app/webrtc.ice     — either peer sends ICE candidate → forwarded to other
 *  6. /app/webrtc.leave   — user leaves, pool updated, other peer notified
 *
 * FIX: userJoin() now calls instantMatchProcessor.startLooking(sub)
 * after adding the user to the pool. Previously the user was added to
 * the pool but the match search was never triggered, so instant matching
 * only worked when called via the REST API — not from the WebSocket join.
 *
 * All responses go to /user/{cognitoSub}/queue/webrtc on the client.
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

    @Autowired
    private IInstantMatchProcessor instantMatchProcessor;   // FIX: wired in

    // ── Step 1: User joins pool + instant match triggered ─────────────────────

    /**
     * Called when a user opens the find-match screen.
     *
     * What happens:
     *   1. User added to UserPoolService (in-memory pool for WebRTC discovery)
     *   2. Available pool list sent back to the joining user
     *   3. InstantMatchProcessor.startLooking() fires — searches Redis presence
     *      set for compatible online users and either:
     *        a) Finds a match → notifies both users via /queue/match → WebRTC starts
     *        b) No match    → enqueues user in Redis waiting queue → notified later
     */
    @MessageMapping("/webrtc.join")
    public void userJoin(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
        String sub = getPrincipalName(headerAccessor);
        log.info("[JOIN] User joined pool: {}", sub);

        User user = resolveUser(sub);

        // Add to in-memory pool so other users can see this user in their pool list
        userPoolService.addUser(new PoolUserVO(
                sub,
                user.getFirstName(),
                user.getLastName()
        ));

        // Send current available user list back to the joining user
        List<PoolUserVO> availableUsers = userPoolService.getOtherUsers(sub);
        messagingTemplate.convertAndSendToUser(sub, USER_QUEUE, availableUsers);
        log.info("[JOIN] Sent pool list ({} users) to {}", availableUsers.size(), sub);

        // FIX: trigger instant match search — marks user as LOOKING in Redis,
        // scans the presence set for compatible candidates, notifies via
        // /user/{sub}/queue/match if found, or enqueues in waiting queue if not.
        try {
            instantMatchProcessor.startLooking(user.getId().toString());
            log.info("[JOIN] Instant match search started for userId: {}", user.getId());
        } catch (MatchmakingException ex) {
            // Non-fatal — user is still in the pool even if search fails
            log.error("ALERT_FOR_ERROR: startLooking failed for sub: {}. Error: {}",
                    sub, ex.getMessage(), ex);
        }
    }

    // ── Step 2: Caller sends connection request ───────────────────────────────

    /**
     * Caller selected a user from the pool and wants to connect.
     * Forward the request to the selected callee user.
     */
    @MessageMapping("/webrtc.request")
    public void connectionRequest(@Payload WebRTCSignalVO signal,
                                  SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        signal.setType("CONNECTION_REQUEST");

        log.info("[REQUEST] {} wants to connect to {}", callerSub, signal.getToUserId());
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ── Step 3: Caller sends SDP offer ───────────────────────────────────────

    /**
     * Callee accepted the request. Caller creates SDP offer and sends it here.
     * Server forwards it to the callee.
     */
    @MessageMapping("/webrtc.offer")
    public void handleOffer(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        signal.setType("OFFER");

        log.info("[OFFER] Forwarding SDP offer from {} to {}", callerSub, signal.getToUserId());
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ── Step 4: Callee sends SDP answer ──────────────────────────────────────

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
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ── Step 5: ICE candidate exchange ───────────────────────────────────────

    /**
     * Either peer generated an ICE candidate.
     * Server forwards it to the other peer.
     * Happens continuously on both sides until P2P is established.
     */
    @MessageMapping("/webrtc.ice")
    public void handleIceCandidate(@Payload WebRTCSignalVO signal,
                                   SimpMessageHeaderAccessor headerAccessor) {
        String senderSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(senderSub);
        signal.setType("ICE_CANDIDATE");

        log.info("[ICE] Forwarding ICE candidate from {} to {}", senderSub, signal.getToUserId());
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ── Step 6: User leaves ───────────────────────────────────────────────────

    /**
     * User left the screen or ended the call.
     * Removed from pool. Other peer notified if they were in a call.
     */
    @MessageMapping("/webrtc.leave")
    public void handleLeave(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String leavingSub = getPrincipalName(headerAccessor);
        userPoolService.removeUser(leavingSub);
        log.info("[LEAVE] User {} left the pool", leavingSub);

        if (signal.getToUserId() != null) {
            WebRTCSignalVO leaveSignal = new WebRTCSignalVO();
            leaveSignal.setType("PEER_LEFT");
            leaveSignal.setFromUserId(leavingSub);
            leaveSignal.setToUserId(signal.getToUserId());

            messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, leaveSignal);
            log.info("[LEAVE] Notified {} that {} has left", signal.getToUserId(), leavingSub);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
