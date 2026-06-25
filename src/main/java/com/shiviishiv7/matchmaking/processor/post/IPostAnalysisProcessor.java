package com.shiviishiv7.matchmaking.processor.post;

import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.vo.post.PostAnalyzeResponseVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostSubmitRequestVO;
import com.shiviishiv7.matchmaking.provider.vo.post.PostSubmitResponseVO;

public interface IPostAnalysisProcessor {
    PostAnalyzeResponseVO analyze(String postText);
    PostSubmitResponseVO submit(String cognitoSub, PostSubmitRequestVO request);
}
