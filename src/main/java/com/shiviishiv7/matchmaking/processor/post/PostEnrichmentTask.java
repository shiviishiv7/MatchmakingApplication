package com.shiviishiv7.matchmaking.processor.post;

import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;

public record PostEnrichmentTask(MatchFilterVO filterVO, Long postId) {}
