package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryProfileRegistryVO {

    private Integer id;
    private Integer userId;
    private MatchCategory matchCategory;
    private Integer extensionProfileId;
    private Integer completionPct;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        if (matchCategory == null) throw new IllegalArgumentException("matchCategory is required.");
        if (extensionProfileId == null) throw new IllegalArgumentException("extensionProfileId is required.");
        return true;
    }
}
