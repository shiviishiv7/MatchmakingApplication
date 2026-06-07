//package com.shiviishiv7.matchmaking.controller.ws;
//
//import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
//import com.shiviishiv7.matchmaking.processor.instant.IInstantMatchProcessor;
//import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
//import com.shiviishiv7.matchmaking.provider.model.User;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
//import org.springframework.stereotype.Controller;
//
//import java.util.Optional;
//import java.util.UUID;
//
///**
// * STOMP message handlers for the find-match flow.
// *
// * Client connects to /ws with JWT token, then sends:
// *   /app/match/start  → user is on the find-screen, start looking
// *   /app/match/stop   → user left the find-screen, stop looking
// *
// * Server responds privately via:
// *   /user/{sub}/queue/match  → MatchNotificationVO (MATCH_FOUND | NO_MATCH_FOUND)
// */
//@Controller
//@Slf4j
//public class MatchmakingWebSocketController {
//
//    @Autowired
//    private IInstantMatchProcessor instantMatchProcessor;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @MessageMapping("/match/start")
//    public void startLooking(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
//        String sub = getPrincipalName(headerAccessor);
//        log.info("WebSocket /match/start received from sub: {}", sub);
//
//        // sub is the Cognito userId — must match User.cognitoSub
//        // For now we resolve by cognitoSub; the processor expects a UUID user ID
//        // This mapping happens inside the processor via userRepository.findByCognitoSub
//        instantMatchProcessor.startLooking(resolveUserId(headerAccessor));
//    }
//
//    @MessageMapping("/match/stop")
//    public void stopLooking(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
//        String sub = getPrincipalName(headerAccessor);
//        log.info("WebSocket /match/stop received from sub: {}", sub);
//        instantMatchProcessor.stopLooking(resolveUserId(headerAccessor));
//    }
//
//    // ── Helpers ───────────────────────────────────────────────────────────
//
//    private String getPrincipalName(SimpMessageHeaderAccessor headerAccessor) {
//        if (headerAccessor.getUser() == null) {
//            throw new IllegalStateException("No authenticated principal found in WebSocket session.");
//        }
//        return headerAccessor.getUser().getName();
//    }
//
//    /**
//     * Resolves the DB user UUID from the Cognito sub stored as the WebSocket principal.
//     */
//    private UUID resolveUserId(SimpMessageHeaderAccessor headerAccessor) throws MatchmakingException {
//        String cognitoSub = getPrincipalName(headerAccessor);
//        Optional<User> optionalUser = userRepository.findByCognitoSub(cognitoSub);
//        if (optionalUser.isEmpty()) {
//            log.error("ALERT_FOR_ERROR: No user found for Cognito sub: {}", cognitoSub);
//            throw new MatchmakingException("Authenticated user not registered in the system", 401);
//        }
//        return optionalUser.get().getId();
//    }
//}
