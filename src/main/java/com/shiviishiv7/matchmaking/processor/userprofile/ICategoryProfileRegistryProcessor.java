package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;

public interface ICategoryProfileRegistryProcessor {

    BaseVO add(CategoryProfileRegistryVO vo) throws MatchmakingException;

    BaseVO update(CategoryProfileRegistryVO vo) throws MatchmakingException;

    BaseVO getById(String id) throws MatchmakingException;

    BaseVO getAllByUserId(String userId) throws MatchmakingException;

    BaseVO getActiveByUserId(String userId) throws MatchmakingException;

    BaseVO deactivate(String userId, String matchCategory) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;
}
