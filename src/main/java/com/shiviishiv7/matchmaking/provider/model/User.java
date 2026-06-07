package com.shiviishiv7.matchmaking.provider.model;


import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * User — a verified employee on the platform.
 * cognitoSub links this record to the Cognito User Pool entry.
 * All sensitive auth data lives in Cognito; this table holds
 * only what is needed for matching and display.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_cognito_sub", columnList = "cognito_sub", unique = true),
        @Index(name = "idx_user_email",       columnList = "email",       unique = true),
        @Index(name = "idx_user_status",      columnList = "status"),
        @Index(name = "idx_user_company",     columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ── Cognito link ──────────────────────────────────────────────────────
    @Column(name = "cognito_sub", nullable = false, unique = true, length = 100)
    private String cognitoSub;                  // Cognito user pool sub (UUID string)

    // ── Identity ──────────────────────────────────────────────────────────
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;                       // verified work email

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;


    @Column(name = "company_id", nullable = false)
    private String companyId;

    // ── Profile attributes (used in matching) ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;              // age calculated dynamically

    @Column(name = "timezone", length = 50)
    private String timezone;                    // IANA e.g. "Asia/Kolkata"

    @Column(name = "industry", length = 100)
    private String industry;                    // user's own industry

    @Column(name = "bio", length = 500)
    private String bio;                         // short self-description

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    // ── Platform state ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

//    // ── Relationships ─────────────────────────────────────────────────────
//    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private UserPreference preference;

//    @ElementCollection(fetch = FetchType.LAZY)
//    @CollectionTable(name = "user_interests",
//            joinColumns = @JoinColumn(name = "user_id"))
//    @Column(name = "interest", length = 80)
//    private List<String> interests;             // e.g. ["hiking", "cooking", "travel"]
}
