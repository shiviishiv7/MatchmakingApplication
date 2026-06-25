package com.shiviishiv7.matchmaking.provider.vo.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PostSubmitRequestVO {
    @NotBlank
    private String postText;
    private List<PostAnswerVO> answers;
}
