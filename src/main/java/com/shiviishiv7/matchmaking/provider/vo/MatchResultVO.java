package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResultVO {

    private Integer id;
    private Integer userId;
    private Integer candidateUserId;
    private MatchCategory matchCategory;
    private Integer compatibilityScore;
    private String scoreBreakdown;
    private MatchStatus status;
    private Boolean isMutual;
    private LocalDateTime shownAt;
    private LocalDateTime actedAt;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        if (candidateUserId == null) throw new IllegalArgumentException("candidateUserId is required.");
        if (matchCategory == null) throw new IllegalArgumentException("matchCategory is required.");
        if (status == null) throw new IllegalArgumentException("status is required.");
        return true;
    }
}
