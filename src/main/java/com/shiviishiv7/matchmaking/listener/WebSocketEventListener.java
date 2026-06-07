package com.shiviishiv7.matchmaking.listener;

import com.shiviishiv7.matchmaking.processor.instant.IInstantMatchProcessor;
import com.shiviishiv7.matchmaking.service.presence.UserPresenceService;
import com.shiviishiv7.matchmaking.service.waiting.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

/**
 * Cleans up Redis presence and waiting queue when a WebSocket session drops
 * (browser closed, network lost, user navigated away).
 */
@Component
@Slf4j
public class WebSocketEventListener {

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (accessor.getUser() == null) return;

        String sub = accessor.getUser().getName();
        log.info("WebSocket disconnected for sub: {}", sub);

        try {
            UUID userId = UUID.fromString(sub);
            userPresenceService.markAsNotLooking(userId);
            waitingQueueService.dequeue(userId);
            log.info("Cleaned up presence and waiting queue for user: {}", userId);
        } catch (IllegalArgumentException ex) {
            log.warn("Could not parse sub as UUID on disconnect: {}", sub);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error cleaning up on WebSocket disconnect for sub: {}. Error: {}", sub, ex.getMessage(), ex);
        }
    }
}
