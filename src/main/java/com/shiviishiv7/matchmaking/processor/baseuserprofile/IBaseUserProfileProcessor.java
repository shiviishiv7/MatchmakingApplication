package com.shiviishiv7.matchmaking.processor.baseuserprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.BaseUserProfileVO;

public interface IBaseUserProfileProcessor {

    BaseVO add(BaseUserProfileVO vo) throws MatchmakingException;

    BaseVO update(BaseUserProfileVO vo) throws MatchmakingException;

    BaseVO getById(String id) throws MatchmakingException;

    BaseVO getByUserId(String userId) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;
    BaseVO get(String id) throws MatchmakingException;
    BaseVO getByEmail(String email) throws MatchmakingException;
}
