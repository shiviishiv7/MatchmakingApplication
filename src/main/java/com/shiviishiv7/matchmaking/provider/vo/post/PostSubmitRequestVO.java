package com.shiviishiv7.matchmaking.provider.vo.post;

import com.shiviishiv7.matchmaking.common.enums.IntentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PostSubmitRequestVO {
    @NotNull
    private IntentType intent;
    @NotBlank
    private String postText;
    private List<PostAnswerVO> answers;
    private PartnerPreferenceRequestVO partnerPreference;
}
