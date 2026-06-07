package com.shiviishiv7.matchmaking.processor.instant;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;

import java.util.UUID;

public interface IInstantMatchProcessor {

    /**
     * Called when a user enters the find-match screen.
     * Marks them as LOOKING in Redis, tries to find a compatible online user,
     * and notifies both via WebSocket. If no match is found, puts them in the waiting queue.
     */
    void startLooking(UUID userId) throws MatchmakingException;

    /**
     * Called when a user leaves the find-match screen or disconnects.
     * Removes them from the LOOKING set and waiting queue.
     */
    void stopLooking(UUID userId) throws MatchmakingException;

    void v2(String sub);
}
