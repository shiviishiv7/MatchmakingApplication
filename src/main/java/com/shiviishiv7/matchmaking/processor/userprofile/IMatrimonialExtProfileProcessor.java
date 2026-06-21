package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.MatrimonialExtProfileVO;

public interface IMatrimonialExtProfileProcessor {

    BaseVO add(MatrimonialExtProfileVO vo) throws MatchmakingException;

    BaseVO update(MatrimonialExtProfileVO vo) throws MatchmakingException;

    BaseVO getById(String id) throws MatchmakingException;

    BaseVO getByUserId(String userId) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;
}
