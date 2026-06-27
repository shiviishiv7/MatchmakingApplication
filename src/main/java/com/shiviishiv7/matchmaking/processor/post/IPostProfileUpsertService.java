package com.shiviishiv7.matchmaking.processor.post;

import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;

public interface IPostProfileUpsertService {
    void upsert(MatchFilterVO filterVO);
}
