package com.shiviishiv7.matchmaking.provider.model;



import com.shiviishiv7.matchmaking.common.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;


/**
 * UserPreference — what this user is looking for in a match.
 * Stored separately from User to keep the users table lean.
 * The matching algorithm reads these when scoring candidates.
 */
@Entity
@Table(name = "userPreferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "cognitoSub", nullable = false)
    private String cognitoSub;

    // ── Age range preference ──────────────────────────────────────────────
    @Column(name = "minAge")
    @Builder.Default
    private Integer minAge = 18;

    @Column(name = "maxAge")
    @Builder.Default
    private Integer maxAge = 60;

    // ── Gender preference ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "preferredGender", length = 20)
    private Gender preferredGender;             // null = no preference

//    // ── Industry preference ───────────────────────────────────────────────
//    @ElementCollection(fetch = FetchType.LAZY)
//    @CollectionTable(name = "preference_industries",
//            joinColumns = @JoinColumn(name = "preference_id"))
//    @Column(name = "industry", length = 100)
//    private List<String> preferredIndustries;   // empty = no preference

//    // ── Timezone tolerance ────────────────────────────────────────────────
//    @Column(name = "max_timezone_offset_hours")
//    @Builder.Default
//    private Integer maxTimezoneOffsetHours = 5; // match within ±5 hours

    // ── Open to same company matches ──────────────────────────────────────
    @Column(name = "same_company_allowed", nullable = false)
    @Builder.Default
    private Boolean sameCompanyAllowed = false; // default: no intra-company matches

    // ── Company preference ────────────────────────────────────────────────
    @Column(name = "preferred_company", length = 200)
    private String preferredCompany;            // null = no preference

    // ── Education preference ──────────────────────────────────────────────
    @Column(name = "preferred_college", length = 200)
    private String preferredCollege;            // null = no preference

    // ── Location preference ───────────────────────────────────────────────
    @Column(name = "preferred_zip", length = 20)
    private String preferredZip;

    @Column(name = "preferred_city", length = 100)
    private String preferredCity;

    @Column(name = "preferred_state", length = 100)
    private String preferredState;

    @Column(name = "preferred_country", length = 100)
    private String preferredCountry;
}
