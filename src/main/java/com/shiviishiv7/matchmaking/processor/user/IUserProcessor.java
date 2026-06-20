package com.shiviishiv7.matchmaking.processor.user;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserVO;



public interface IUserProcessor {

    BaseVO add(UserVO userVO) throws MatchmakingException;

    BaseVO update(UserVO userVO) throws MatchmakingException;

    BaseVO get(String id) throws MatchmakingException;

    BaseVO getByEmail(String email) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;
}
