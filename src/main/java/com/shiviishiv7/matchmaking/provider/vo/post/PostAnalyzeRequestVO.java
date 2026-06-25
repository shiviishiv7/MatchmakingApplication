package com.shiviishiv7.matchmaking.provider.vo.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostAnalyzeRequestVO {
    @NotBlank
    private String postText;
}
