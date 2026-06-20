package com.shiviishiv7.matchmaking.processor.match;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchVO;



public interface IMatchProcessor {

    BaseVO add(MatchVO matchVO) throws MatchmakingException;

    BaseVO get(String id) throws MatchmakingException;

    BaseVO getActiveMatchForUser(String userId) throws MatchmakingException;

    BaseVO getAllByStatus(String status) throws MatchmakingException;

    BaseVO end(String id) throws MatchmakingException;
}
