package com.shiviishiv7.matchmaking.provider.vo.post;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostSubmitResponseVO {
    private Long postId;
    private MatchCategory inferredCategory;
    private String categoryDisplayName;
    private Boolean profileUpdated;
}
