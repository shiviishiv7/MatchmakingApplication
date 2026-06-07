package com.shiviishiv7.matchmaking.processor.match;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchVO;

import java.util.UUID;

public interface IMatchProcessor {

    BaseVO add(MatchVO matchVO) throws MatchmakingException;

    BaseVO get(UUID id) throws MatchmakingException;

    BaseVO getActiveMatchForUser(UUID userId) throws MatchmakingException;

    BaseVO getAllByStatus(String status) throws MatchmakingException;

    BaseVO end(UUID id) throws MatchmakingException;
}
