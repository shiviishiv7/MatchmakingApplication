package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Incoming request from the frontend when a user opens the discovery screen.
 */
@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDiscoveryRequestVO {

    private Integer userId;
    private MatchCategory matchCategory;
    private int page;           // 0-indexed
    private int pageSize;       // default 20

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        if (matchCategory == null) throw new IllegalArgumentException("matchCategory is required.");
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        if (page < 0) page = 0;
        return true;
    }
}
