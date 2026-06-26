package com.shiviishiv7.matchmaking.provider.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a parent group (e.g. "Health & Wellness").
 * Replaces the hard-coded parentGroup string inside MatchCategory enum.
 */
@Entity
@Table(name = "MATCH_CATEGORY_GROUP")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchCategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "displayOrder")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<MatchCategoryEntity> categories;
}
