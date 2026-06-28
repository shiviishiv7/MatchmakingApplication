package com.shiviishiv7.matchmaking.provider.vo.post;

import com.shiviishiv7.matchmaking.common.enums.IntentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostAnalyzeRequestVO {
    @NotNull
    private IntentType intent;
    @NotBlank
    private String postText;
}
