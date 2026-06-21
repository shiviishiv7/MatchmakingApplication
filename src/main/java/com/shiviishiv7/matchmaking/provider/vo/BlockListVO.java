package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockListVO {

    private Integer id;
    private Integer blockerId;
    private Integer blockedId;
    private String reason;
    private LocalDateTime blockedAt;

    public boolean validate() {
        if (blockerId == null) throw new IllegalArgumentException("blockerId is required.");
        if (blockedId == null) throw new IllegalArgumentException("blockedId is required.");
        if (blockerId.equals(blockedId)) throw new IllegalArgumentException("Cannot block yourself.");
        return true;
    }
}
