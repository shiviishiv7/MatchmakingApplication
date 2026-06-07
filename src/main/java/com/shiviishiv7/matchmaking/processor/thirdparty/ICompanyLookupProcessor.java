package com.shiviishiv7.matchmaking.processor.thirdparty;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;

public interface ICompanyLookupProcessor {

    /**
     * Searches for a company by name.
     * Checks the local DB first; falls back to Clearbit if nothing is found locally.
     */
    BaseVO searchForSignup(String name) throws MatchmakingException;
}
