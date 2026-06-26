package com.shiviishiv7.matchmaking.provider.vo.category;

import com.shiviishiv7.matchmaking.provider.model.MatchCategoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchCategoryVO {

    private Integer id;
    private String enumKey;
    private String displayName;
    private Integer groupId;
    private String groupName;

    public static MatchCategoryVO from(MatchCategoryEntity entity) {
        return MatchCategoryVO.builder()
                .id(entity.getId())
                .enumKey(entity.getEnumKey())
                .displayName(entity.getDisplayName())
                .groupId(entity.getGroup().getId())
                .groupName(entity.getGroup().getName())
                .build();
    }
}
