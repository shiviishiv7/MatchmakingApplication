package com.shiviishiv7.matchmaking.provider.vo.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PostQuestionVO {
    private String id;
    private String pairGroup; // null = unpaired (renders full-width)
    private String question;
    /** city | single_choice | multi_choice | range | boolean | dropdown */
    private String type;
    private List<String> options;
    private String placeholder;
    private Integer min;
    private Integer max;
}
