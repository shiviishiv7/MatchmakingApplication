package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * DB-backed match category. The enumKey field maps 1-to-1 with MatchCategory enum names
 * (e.g. "GYM_PARTNER") so existing columns storing the enum as VARCHAR remain valid.
 * New categories can be added via INSERT without a code redeploy.
 */
@Entity
@Table(name = "MATCH_CATEGORY", indexes = {
        @Index(name = "idx_mc_enum_key", columnList = "enumKey", unique = true),
        @Index(name = "idx_mc_group_id", columnList = "group_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private MatchCategoryGroup group;

    /** Stable identifier — matches the Java enum name (e.g. GYM_PARTNER). */
    @Column(name = "enumKey", nullable = false, unique = true, length = 60)
    private String enumKey;

    @Column(name = "displayName", nullable = false, length = 150)
    private String displayName;

    @Column(name = "displayOrder")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;
}
