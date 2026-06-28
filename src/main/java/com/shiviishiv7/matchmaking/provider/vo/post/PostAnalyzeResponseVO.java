package com.shiviishiv7.matchmaking.provider.vo.post;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostAnalyzeResponseVO {
    private MatchCategory inferredCategory;
    private String categoryDisplayName;
    private List<PostQuestionPairVO> questions;
}
