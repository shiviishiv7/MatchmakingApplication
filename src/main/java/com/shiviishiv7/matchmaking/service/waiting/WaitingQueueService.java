package com.shiviishiv7.matchmaking.service.waiting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Manages users who looked for an instant match but found no one.
 * Backed by a Redis Sorted Set scored by entry timestamp — oldest waiters
 * are matched first (FIFO within compatible candidates).
 */
@Service
@Slf4j
public class WaitingQueueService {

    private static final String WAITING_KEY = "waiting:queue";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void enqueue(UUID userId) {
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(WAITING_KEY, userId.toString(), score);
        log.info("User {} added to waiting queue at score {}.", userId, score);
    }

    public void dequeue(UUID userId) {
        redisTemplate.opsForZSet().remove(WAITING_KEY, userId.toString());
        log.info("User {} removed from waiting queue.", userId);
    }

    public boolean isWaiting(UUID userId) {
        Double score = redisTemplate.opsForZSet().score(WAITING_KEY, userId.toString());
        return score != null;
    }

    /**
     * Returns all waiting user IDs ordered by wait time (oldest first).
     */
    public Set<String> getAllWaiting() {
        Set<String> members = redisTemplate.opsForZSet().range(WAITING_KEY, 0, -1);
        return members != null ? members : Collections.emptySet();
    }
}
