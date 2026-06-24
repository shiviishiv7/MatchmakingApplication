package com.shiviishiv7.matchmaking.controller.ws;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.vo.ws.InstantSearchFilterVO;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handles WebRTC signaling over STOMP WebSocket.
 *
 * Full flow:
 *  1. /app/webrtc.join     — user joins pool; receives no-filter pool list; pending requesters notified if new user matches
 *  2. /app/webrtc.search   — user sends filter; receives filtered pool list or NO_MATCH_NOW
 *  3. /app/webrtc.request  — caller sends connection request to a specific user
 *  4. /app/webrtc.busy     — callee is in a call; server notifies caller with BUSY
 *  5. /app/webrtc.offer    — caller sends SDP offer → forwarded to callee
 *  6. /app/webrtc.answer   — callee sends SDP answer → forwarded to caller; both marked busy
 *  7. /app/webrtc.ice      — either peer sends ICE candidate → forwarded to other peer
 *  8. /app/webrtc.leave    — user leaves; pool updated; other peer notified
 *
 * All responses go to /user/{sub}/queue/webrtc on the client.
 */
@Controller
@Slf4j
public class MatchmakingWebSocketController {

    private static final String USER_QUEUE = "/queue/webrtc";

    @Autowired private SimpMessagingTemplate           messagingTemplate;
    @Autowired private BaseUserProfileRepository       baseUserProfileRepository;
    @Autowired private CategoryProfileRegistryRepository categoryProfileRegistryRepository;
    @Autowired private UserPoolService                 userPoolService;

    // ─── Step 1: User Joins Pool ──────────────────────────────────────────────

    @MessageMapping("/webrtc.join")
    public void userJoin(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
        String sub = getPrincipalName(headerAccessor);

        if (!userPoolService.isInPool(sub)) {
            PoolUserVO poolUser = buildPoolUser(sub);
            userPoolService.addUser(poolUser);
            log.info("[JOIN] User added to pool: {}", sub);

            // Notify any pending filter requesters who are satisfied by this new joiner
            Map<String, PoolUserVO> pendingMatches = userPoolService.findPendingMatches(poolUser);
            pendingMatches.forEach((requesterSub, joiner) -> {
                WebRTCSignalVO matchSignal = new WebRTCSignalVO();
                matchSignal.setType("MATCH_FOUND");
                matchSignal.setFromUserId("server");
                matchSignal.setToUserId(requesterSub);
                messagingTemplate.convertAndSendToUser(requesterSub, USER_QUEUE, joiner);
                log.info("[JOIN] Notified pending requester {} of new joiner {}", requesterSub, sub);
            });
        } else {
            log.info("[JOIN] User already in pool, skipping add: {}", sub);
        }

        List<PoolUserVO> available = userPoolService.getOtherUsers(sub);
        userPoolService.markAsSeen(sub, available);
        messagingTemplate.convertAndSendToUser(sub, USER_QUEUE, available);
        log.info("[JOIN] Sent no-filter pool list ({} users) to {}", available.size(), sub);
    }

    // ─── Step 2: Filtered Search ──────────────────────────────────────────────

    /**
     * Client sends a filter payload.
     * childCategory present → advanced filter (category + basic fields).
     * childCategory absent  → basic filter (gender + city + age).
     *
     * If results found:  sends POOL_LIST (same format as join).
     * If no results:     saves pending request, sends NO_MATCH_NOW signal.
     */
    @MessageMapping("/webrtc.search")
    public void filterSearch(@Payload InstantSearchFilterVO filter,
                             SimpMessageHeaderAccessor headerAccessor) {
        String sub = getPrincipalName(headerAccessor);
        boolean isAdvanced = filter.getChildCategory() != null && !filter.getChildCategory().isBlank();
        log.info("[SEARCH] {} requested {} search", sub, isAdvanced ? "advanced" : "basic");

        List<PoolUserVO> results = userPoolService.getFilteredUsers(sub, filter);

        if (!results.isEmpty()) {
            userPoolService.markAsSeen(sub, results);
            userPoolService.removePendingRequest(sub); // clear any previous pending request
            messagingTemplate.convertAndSendToUser(sub, USER_QUEUE, results);
            log.info("[SEARCH] Sent {} filtered users to {}", results.size(), sub);
        } else {
            log.info("[SEARCH] No match found for {} — saving pending request", sub);
            userPoolService.addPendingRequest(sub, filter);

            WebRTCSignalVO noMatch = new WebRTCSignalVO();
            noMatch.setType("NO_MATCH_NOW");
            noMatch.setFromUserId("server");
            noMatch.setToUserId(sub);
            messagingTemplate.convertAndSendToUser(sub, USER_QUEUE, noMatch);
        }
    }

    // ─── Step 3: Caller sends Connection Request ──────────────────────────────

    @MessageMapping("/webrtc.request")
    public void connectionRequest(@Payload WebRTCSignalVO signal,
                                  SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        String targetSub = signal.getToUserId();

        if (userPoolService.isBusy(targetSub)) {
            log.info("[REQUEST] {} is busy, notifying caller {}", targetSub, callerSub);
            WebRTCSignalVO busy = new WebRTCSignalVO();
            busy.setType("BUSY");
            busy.setFromUserId(targetSub);
            busy.setToUserId(callerSub);
            messagingTemplate.convertAndSendToUser(callerSub, USER_QUEUE, busy);
            return;
        }

        signal.setType("CONNECTION_REQUEST");
        log.info("[REQUEST] {} wants to connect to {}", callerSub, targetSub);
        messagingTemplate.convertAndSendToUser(targetSub, USER_QUEUE, signal);
    }

    // ─── Step 4: Callee is busy ───────────────────────────────────────────────

    /**
     * Callee is in an active call — forward BUSY signal to the original caller.
     */
    @MessageMapping("/webrtc.busy")
    public void connectionBusy(@Payload WebRTCSignalVO signal,
                               SimpMessageHeaderAccessor headerAccessor) {
        String calleeSub = getPrincipalName(headerAccessor);
        String callerSub = signal.getToUserId();

        log.info("[BUSY] {} is busy, notifying caller {}", calleeSub, callerSub);

        WebRTCSignalVO busy = new WebRTCSignalVO();
        busy.setType("BUSY");
        busy.setFromUserId(calleeSub);
        busy.setToUserId(callerSub);
        messagingTemplate.convertAndSendToUser(callerSub, USER_QUEUE, busy);
    }

    // ─── Step 5: Caller sends SDP Offer ──────────────────────────────────────

    @MessageMapping("/webrtc.offer")
    public void handleOffer(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String callerSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(callerSub);
        signal.setType("OFFER");
        log.info("[OFFER] Forwarding SDP offer from {} to {}", callerSub, signal.getToUserId());
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 6: Callee sends SDP Answer ─────────────────────────────────────

    @MessageMapping("/webrtc.answer")
    public void handleAnswer(@Payload WebRTCSignalVO signal,
                             SimpMessageHeaderAccessor headerAccessor) {
        String calleeSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(calleeSub);
        signal.setType("ANSWER");

        // Both sides are now in an active call
        userPoolService.markBusy(calleeSub);
        userPoolService.markBusy(signal.getToUserId());
        log.info("[ANSWER] Marking {} and {} as busy", calleeSub, signal.getToUserId());

        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 7: ICE Candidate Exchange ──────────────────────────────────────

    @MessageMapping("/webrtc.ice")
    public void handleIceCandidate(@Payload WebRTCSignalVO signal,
                                   SimpMessageHeaderAccessor headerAccessor) {
        String senderSub = getPrincipalName(headerAccessor);
        signal.setFromUserId(senderSub);
        signal.setType("ICE_CANDIDATE");
        log.info("[ICE] Forwarding ICE candidate from {} to {}", senderSub, signal.getToUserId());
        messagingTemplate.convertAndSendToUser(signal.getToUserId(), USER_QUEUE, signal);
    }

    // ─── Step 8: User Leaves ─────────────────────────────────────────────────

    @MessageMapping("/webrtc.leave")
    public void handleLeave(@Payload WebRTCSignalVO signal,
                            SimpMessageHeaderAccessor headerAccessor) {
        String leavingSub = getPrincipalName(headerAccessor);
        userPoolService.removeUser(leavingSub); // also clears busy, seen, pending
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

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getPrincipalName(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() == null) {
            throw new IllegalStateException("No authenticated principal in WebSocket session.");
        }
        return headerAccessor.getUser().getName();
    }

    private BaseUserProfile resolveUser(String cognitoSub) throws MatchmakingException {
        Optional<BaseUserProfile> user = baseUserProfileRepository.findByCognitoSub(cognitoSub);
        if (user.isEmpty()) {
            throw new MatchmakingException("User not found for sub: " + cognitoSub, 401);
        }
        return user.get();
    }

    /** Builds a fully enriched PoolUserVO by loading profile + active categories from DB. */
    private PoolUserVO buildPoolUser(String sub) throws MatchmakingException {
        BaseUserProfile profile = resolveUser(sub);

        List<String> categories = categoryProfileRegistryRepository
                .findByCognitoSubAndIsActive(sub, true)
                .stream()
                .map(CategoryProfileRegistry::getMatchCategory)
                .map(Enum::name)
                .collect(Collectors.toList());

        return PoolUserVO.builder()
                .cognitoSub(sub)
                .firstName(profile.getName())
                .lastName(profile.getName())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .currentCity(profile.getCurrentCity())
                .dateOfBirth(profile.getDateOfBirth())
                .matchCategories(categories)
                .build();
    }
}
