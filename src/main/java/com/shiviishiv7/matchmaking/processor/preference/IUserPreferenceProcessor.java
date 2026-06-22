package com.shiviishiv7.matchmaking.processor.preference;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;

public interface IUserPreferenceProcessor {

    BaseVO save(MatchFilterVO vo) throws MatchmakingException;

    BaseVO getByUserId(String cognitoSub) throws MatchmakingException;
}
