package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.PartnerPreferenceVO;

public interface IPartnerPreferenceProcessor {

    BaseVO add(PartnerPreferenceVO vo) throws MatchmakingException;

    BaseVO update(PartnerPreferenceVO vo) throws MatchmakingException;

    BaseVO getById(String id) throws MatchmakingException;

    BaseVO getByUserId(String userId) throws MatchmakingException;

    BaseVO delete(String id) throws MatchmakingException;
}
