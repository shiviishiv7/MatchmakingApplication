package com.shiviishiv7.matchmaking.scheduler;

import com.shiviishiv7.matchmaking.processor.instant.IInstantMatchProcessor;
import com.shiviishiv7.matchmaking.service.presence.UserPresenceService;
import com.shiviishiv7.matchmaking.service.waiting.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;


/**
 * Runs every 30 seconds.
 * When a new user comes online (enters LOOKING set), checks the waiting queue
 * for users who previously found no match — re-runs their search so they get
 * notified as soon as a compatible person becomes available.
 */
@Component
@Slf4j
public class WaitingQueueMatcherJob {

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private IInstantMatchProcessor instantMatchProcessor;

    @Scheduled(fixedDelay = 30_000)
    public void retryWaitingUsers() {
        try {
            log.debug("WaitingQueueMatcherJob tick");
            Set<String> waitingUserIds = waitingQueueService.getAllWaiting();
            if (waitingUserIds.isEmpty()) return;

            long lookingCount = userPresenceService.countLooking();
            if (lookingCount == 0) return;

            log.info("WaitingQueueMatcherJob: {} waiting user(s), {} looking user(s). Retrying matches.", waitingUserIds.size(), lookingCount);

            for (String userIdStr : waitingUserIds) {
                try {
//                    UUID userId = UUID.fromString(userIdStr);
                    waitingQueueService.dequeue(userIdStr);
                    instantMatchProcessor.startLooking(userIdStr);
                } catch (Exception ex) {
                    log.error("ALERT_FOR_ERROR: WaitingQueueMatcherJob failed for user: {}. Error: {}", userIdStr, ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            log.warn("WaitingQueueMatcherJob tick failed: {}", ex.getMessage());
        }
    }
}
