//package com.shiviishiv7.matchmaking.controller.ws;
//
//import com.shiviishiv7.matchmaking.provider.vo.ws.WebRTCSignalVO;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//
///**
// * Forwards WebRTC signaling messages between two peers.
// * The server is purely a relay — it never inspects the SDP/ICE payload.
// *
// * Client sends to:  /app/webrtc/signal
// * Server routes to: /user/{toUserId}/queue/webrtc
// *
// * Signal types the client sends:
// *   OFFER         — initiating peer sends SDP offer
// *   ANSWER        — receiving peer replies with SDP answer
// *   ICE_CANDIDATE — both peers exchange ICE candidates
// *   PEER_LEFT     — either peer ended or lost connection
// */
//@Controller
//@Slf4j
//public class WebRTCSignalingController {
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @MessageMapping("/webrtc/signal")
//    public void relay(@Payload WebRTCSignalVO signal) {
//        if (signal.getToUserId() == null || signal.getToUserId().isBlank()) {
//            log.warn("WebRTC signal missing toUserId — dropping. type: {}, from: {}", signal.getType(), signal.getFromUserId());
//            return;
//        }
//
//        log.info("Relaying WebRTC signal type: {} from: {} to: {} matchId: {}",
//                signal.getType(), signal.getFromUserId(), signal.getToUserId(), signal.getMatchId());
//
//        messagingTemplate.convertAndSendToUser(
//                signal.getToUserId(),
//                "/queue/webrtc",
//                signal
//        );
//    }
//}
