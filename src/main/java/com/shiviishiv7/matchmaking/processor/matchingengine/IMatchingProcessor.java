package com.shiviishiv7.matchmaking.processor.matchingengine;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;

public interface IMatchingProcessor {

    /** Runs the match engine for the given user + category and schedules meetings */
    BaseVO discover(MatchDiscoveryRequestVO request) throws MatchmakingException;

    /** Block a user — they disappear from all future discovery results */
    BaseVO block(String userId, String targetUserId, String reason) throws MatchmakingException;

    /** Unblock a previously blocked user */
    BaseVO unblock(String userId, String targetUserId) throws MatchmakingException;
}
