package com.shiviishiv7.matchmaking.processor.matchengine;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchDiscoveryRequestVO;

public interface IMatchingProcessor {

    /** Returns paginated discovery results for the given user + category */
    BaseVO discover(MatchDiscoveryRequestVO request) throws MatchmakingException;

    /** User liked a match card */
    BaseVO like(String userId, String candidateUserId, String matchCategory) throws MatchmakingException;

    /** User skipped a match card */
    BaseVO skip(String userId, String candidateUserId, String matchCategory) throws MatchmakingException;

    /** Fetch all CONNECTED (mutual) matches for a user */
    BaseVO getConnections(String userId, String matchCategory) throws MatchmakingException;

    /** Block a user — they disappear from all future discovery results */
    BaseVO block(String userId, String targetUserId, String reason) throws MatchmakingException;

    /** Unblock a previously blocked user */
    BaseVO unblock(String userId, String targetUserId) throws MatchmakingException;
}
