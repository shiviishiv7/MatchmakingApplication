package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
import com.shiviishiv7.matchmaking.provider.vo.FitnessExtProfileVO;

public interface IFitnessExtProfileProcessor {

    BaseVO add(FitnessExtProfileVO vo) throws MatchmakingException;

    BaseVO update(FitnessExtProfileVO vo) throws MatchmakingException;

    BaseVO getByCognitoSub(String cognitoSub) throws MatchmakingException;

    BaseVO getByUserId(String userId) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;

    void upsertFromFilter(MatchFilterVO vo) throws MatchmakingException;
}
