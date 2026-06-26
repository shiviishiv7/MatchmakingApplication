package com.shiviishiv7.matchmaking.provider.vo.category;

import com.shiviishiv7.matchmaking.provider.model.MatchCategoryGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchCategoryGroupVO {

    private Integer id;
    private String name;
    private Integer displayOrder;
    private List<MatchCategoryVO> categories;

    public static MatchCategoryGroupVO from(MatchCategoryGroup group, List<MatchCategoryVO> categories) {
        return MatchCategoryGroupVO.builder()
                .id(group.getId())
                .name(group.getName())
                .displayOrder(group.getDisplayOrder())
                .categories(categories)
                .build();
    }
}
