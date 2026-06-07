package com.shiviishiv7.matchmaking.provider.model;



import com.shiviishiv7.matchmaking.common.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * UserPreference — what this user is looking for in a match.
 * Stored separately from User to keep the users table lean.
 * The matching algorithm reads these when scoring candidates.
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── Age range preference ──────────────────────────────────────────────
    @Column(name = "min_age")
    @Builder.Default
    private Integer minAge = 18;

    @Column(name = "max_age")
    @Builder.Default
    private Integer maxAge = 60;

    // ── Gender preference ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_gender", length = 20)
    private Gender preferredGender;             // null = no preference

    // ── Industry preference ───────────────────────────────────────────────
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "preference_industries",
            joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "industry", length = 100)
    private List<String> preferredIndustries;   // empty = no preference

    // ── Timezone tolerance ────────────────────────────────────────────────
    @Column(name = "max_timezone_offset_hours")
    @Builder.Default
    private Integer maxTimezoneOffsetHours = 5; // match within ±5 hours

    // ── Open to same company matches ──────────────────────────────────────
    @Column(name = "same_company_allowed", nullable = false)
    @Builder.Default
    private Boolean sameCompanyAllowed = false; // default: no intra-company matches
}
