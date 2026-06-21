package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "categoryProfileRegistry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "matchCategory"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryProfileRegistry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "userId", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "matchCategory", nullable = false, length = 60)
    private MatchCategory matchCategory;

    @Column(name = "extensionProfileId", nullable = false)
    private Integer extensionProfileId;

    @Column(name = "completionPct")
    @Builder.Default
    private Integer completionPct = 0;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    public CategoryProfileRegistry fromVO(CategoryProfileRegistryVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setUserId(vo.getUserId());
        this.setMatchCategory(vo.getMatchCategory());
        this.setExtensionProfileId(vo.getExtensionProfileId());
        this.setCompletionPct(vo.getCompletionPct());
        this.setIsActive(vo.getIsActive());
        this.setCreatedAt(vo.getCreatedAt());
        return this;
    }

    public CategoryProfileRegistryVO toVO() {
        CategoryProfileRegistryVO vo = new CategoryProfileRegistryVO();
        vo.setId(this.getId());
        vo.setUserId(this.getUserId());
        vo.setMatchCategory(this.getMatchCategory());
        vo.setExtensionProfileId(this.getExtensionProfileId());
        vo.setCompletionPct(this.getCompletionPct());
        vo.setIsActive(this.getIsActive());
        vo.setCreatedAt(this.getCreatedAt());
        return vo;
    }
}
