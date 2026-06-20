package com.shiviishiv7.matchmaking.provider.model;


import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.vo.UserVO;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;


/**
 * User — a verified employee on the platform.
 * cognitoSub links this record to the Cognito User Pool entry.
 * All sensitive auth data lives in Cognito; this table holds
 * only what is needed for matching and display.
 */
@Entity
@Table(name = "\"USER\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;
    // ── Cognito link ──────────────────────────────────────────────────────
    @Column(name = "cognitoSub", nullable = false, unique = true, length = 100)
    private String cognitoSub;

    // ── Identity ──────────────────────────────────────────────────────────
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;                       // verified work email

    @Column(name = "firstName", nullable = false, length = 80)
    private String firstName;

    @Column(name = "lastName", length = 80)
    private String lastName;

    @Column(name = "addressId")
    private String addressId;
    @Column(name = "companyId", nullable = false)
    private String companyId;

    // ── Profile attributes (used in matching) ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "dob")
    private LocalDate dateOfBirth;              // age calculated dynamically

    @Column(name = "age")
    private int age;              // age calculated dynamically


//    @Column(name = "timezone", length = 50)
//    private String timezone;                    // IANA e.g. "Asia/Kolkata"
//
//    @Column(name = "industry", length = 100)
//    private String industry;                    // user's own industry

//    @Column(name = "bio", length = 500)
//    private String bio;                         // short self-description

//    @Column(name = "profile_picture_url", length = 500)
//    private String profilePictureUrl;

    // ── Platform state ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "isActive", nullable = false)
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
    public User fromVO(UserVO vo) {
        if (vo == null) {
            return null;
        }
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setEmail(vo.getEmail());
        this.setFirstName(vo.getFirstName());
        this.setLastName(vo.getLastName());
        if (ObjectUtils.isNotEmpty(vo.getAddressVO()) && ObjectUtils.isNotEmpty(vo.getAddressVO().getId())) {
            this.setAddressId(vo.getAddressVO().getId().toString());
        }
        this.setCompanyId(vo.getCompanyId());
        this.setGender(vo.getGender());
        this.setDateOfBirth(vo.getDateOfBirth());
        this.setAge(vo.getAge());
        this.setStatus(vo.getStatus());
        this.setIsActive(vo.getIsActive());
        return this;
    }

    public UserVO toVO() {
        UserVO vo = new UserVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setEmail(this.getEmail());
        vo.setFirstName(this.getFirstName());
        vo.setLastName(this.getLastName());
        vo.setCompanyId(this.getCompanyId());
        vo.setGender(this.getGender());
        vo.setDateOfBirth(this.getDateOfBirth());
        vo.setAge(this.getAge());
        vo.setStatus(this.getStatus());
        vo.setIsActive(this.getIsActive());
        return vo;
    }
}
