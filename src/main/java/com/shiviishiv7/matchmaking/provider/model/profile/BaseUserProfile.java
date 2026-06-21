package com.shiviishiv7.matchmaking.provider.model.profile;

import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.provider.model.BaseEntity;
import com.shiviishiv7.matchmaking.provider.vo.BaseUserProfileVO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "BASE_USER_PROFILES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BaseUserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "userId", nullable = false, unique = true)
    private String cognitoSub;
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "displayName", nullable = false, length = 100)
    private String displayName;

    @Column(name = "dateOfBirth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "currentCity", length = 100)
    private String currentCity;

    @Column(name = "currentState", length = 100)
    private String currentState;

    @Column(name = "currentCountry", length = 100)
    private String currentCountry;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "profilePhotoUrl", length = 500)
    private String profilePhotoUrl;

    @Column(name = "isPhotoVerified")
    @Builder.Default
    private Boolean isPhotoVerified = false;

    @Column(name = "tagline", length = 200)
    private String tagline;

    @Column(name = "aboutMe", columnDefinition = "TEXT")
    private String aboutMe;

    @Column(name = "languagesKnown", length = 300)
    private String languagesKnown;

    @Column(name = "isProfileVerified")
    @Builder.Default
    private Boolean isProfileVerified = false;

    @Column(name = "isActive")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "lastActiveAt")
    private LocalDateTime lastActiveAt;

    public BaseUserProfile fromVO(BaseUserProfileVO vo) {
        if (vo == null) return null;
        this.setId(vo.getId());
        this.setCognitoSub(vo.getCognitoSub());
        this.setDisplayName(vo.getDisplayName());
        this.setDateOfBirth(vo.getDateOfBirth());
        this.setGender(vo.getGender());
        this.setCurrentCity(vo.getCurrentCity());
        this.setCurrentState(vo.getCurrentState());
        this.setCurrentCountry(vo.getCurrentCountry());
        this.setLatitude(vo.getLatitude());
        this.setLongitude(vo.getLongitude());
        this.setProfilePhotoUrl(vo.getProfilePhotoUrl());
        this.setIsPhotoVerified(vo.getIsPhotoVerified());
        this.setTagline(vo.getTagline());
        this.setAboutMe(vo.getAboutMe());
        this.setLanguagesKnown(vo.getLanguagesKnown());
        this.setIsProfileVerified(vo.getIsProfileVerified());
        this.setIsActive(vo.getIsActive());
        this.setLastActiveAt(vo.getLastActiveAt());
        return this;
    }

    public BaseUserProfileVO toVO() {
        BaseUserProfileVO vo = new BaseUserProfileVO();
        vo.setId(this.getId());
        vo.setCognitoSub(this.getCognitoSub());
        vo.setDisplayName(this.getDisplayName());
        vo.setDateOfBirth(this.getDateOfBirth());
        vo.setGender(this.getGender());
        vo.setCurrentCity(this.getCurrentCity());
        vo.setCurrentState(this.getCurrentState());
        vo.setCurrentCountry(this.getCurrentCountry());
        vo.setLatitude(this.getLatitude());
        vo.setLongitude(this.getLongitude());
        vo.setProfilePhotoUrl(this.getProfilePhotoUrl());
        vo.setIsPhotoVerified(this.getIsPhotoVerified());
        vo.setTagline(this.getTagline());
        vo.setAboutMe(this.getAboutMe());
        vo.setLanguagesKnown(this.getLanguagesKnown());
        vo.setIsProfileVerified(this.getIsProfileVerified());
        vo.setIsActive(this.getIsActive());
        vo.setLastActiveAt(this.getLastActiveAt());
        return vo;
    }
}
