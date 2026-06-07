package com.shiviishiv7.matchmaking.service.presence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks users currently on the "find a match" screen using a Redis Set.
 * A user is "looking" only while their WebSocket session is open on that screen.
 */
@Service
@Slf4j
public class UserPresenceService {

    private static final String LOOKING_KEY = "presence:looking";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void markAsLooking(UUID userId) {
        redisTemplate.opsForSet().add(LOOKING_KEY, userId.toString());
        log.info("User {} marked as LOOKING.", userId);
    }

    public void markAsNotLooking(UUID userId) {
        redisTemplate.opsForSet().remove(LOOKING_KEY, userId.toString());
        log.info("User {} removed from LOOKING set.", userId);
    }

    public boolean isLooking(UUID userId) {
        Boolean result = redisTemplate.opsForSet().isMember(LOOKING_KEY, userId.toString());
        return Boolean.TRUE.equals(result);
    }

    public Set<String> getAllLookingUserIds() {
        Set<String> members = redisTemplate.opsForSet().members(LOOKING_KEY);
        return members != null ? members : Collections.emptySet();
    }

    public long countLooking() {
        Long size = redisTemplate.opsForSet().size(LOOKING_KEY);
        return size != null ? size : 0L;
    }
}
