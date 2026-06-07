package com.shiviishiv7.matchmaking.processor.company;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CompanyVO;

import java.util.UUID;

public interface ICompanyProcessor {

    BaseVO add(CompanyVO companyVO) throws MatchmakingException;

    BaseVO update(CompanyVO companyVO) throws MatchmakingException;

    BaseVO get(UUID id) throws MatchmakingException;

    BaseVO getAll() throws MatchmakingException;

    BaseVO search(String name) throws MatchmakingException;

    BaseVO delete(UUID id) throws MatchmakingException;
}
