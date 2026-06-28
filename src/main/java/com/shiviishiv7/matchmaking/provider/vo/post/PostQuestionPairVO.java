package com.shiviishiv7.matchmaking.provider.vo.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostQuestionPairVO {
    private PostQuestionVO aboutYou;
    private PostQuestionVO partnerPref; // null for unpaired / full-width questions
}
