package com.shiviishiv7.matchmaking.processor.preference;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.UserPreferenceVO;



public interface IUserPreferenceProcessor {

    BaseVO add(UserPreferenceVO preferenceVO) throws MatchmakingException;

    BaseVO update(UserPreferenceVO preferenceVO) throws MatchmakingException;

    BaseVO getByUserId(String userId) throws MatchmakingException;
}
