package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CATEGORY_PROFILE_REGISTRY", indexes = {
        @Index(name = "idx_cpr_cognito_sub", columnList = "cognitoSub"),
        @Index(name = "idx_cpr_sub_category", columnList = "cognitoSub, matchCategory", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryProfileRegistry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    @Enumerated(EnumType.STRING)
    @Column(name = "matchCategory", nullable = false, length = 60)
    private MatchCategory matchCategory;

    // ── Common filter fields ──────────────────────────────────────────────────
    @Column(name = "preferredGender", length = 20)
    private String preferredGender;

    @Column(name = "preferredCity", length = 100)
    private String preferredCity;

    @Column(name = "preferredState", length = 100)
    private String preferredState;

    @Column(name = "preferredCountry", length = 100)
    private String preferredCountry;

    @Column(name = "maxTimezoneOffsetHours")
    private Integer maxTimezoneOffsetHours;

    @Column(name = "sameCompanyAllowed")
    @Builder.Default
    private Boolean sameCompanyAllowed = false;

    // ── Registry metadata ─────────────────────────────────────────────────────
    @Column(name = "completionPct")
    @Builder.Default
    private Integer completionPct = 0;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    public CategoryProfileRegistryVO toVO() {
        CategoryProfileRegistryVO vo = new CategoryProfileRegistryVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setMatchCategory(this.getMatchCategory());
        vo.setPreferredGender(this.getPreferredGender());
        vo.setPreferredCity(this.getPreferredCity());
        vo.setPreferredState(this.getPreferredState());
        vo.setPreferredCountry(this.getPreferredCountry());
        vo.setMaxTimezoneOffsetHours(this.getMaxTimezoneOffsetHours());
        vo.setSameCompanyAllowed(this.getSameCompanyAllowed());
        vo.setCompletionPct(this.getCompletionPct());
        vo.setIsActive(this.getIsActive());
        vo.setCreatedAt(this.getCreatedAt());
        return vo;
    }
}
