package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseUserProfileVO {

    private Integer id;
    private String cognitoSub;
    private String name;
    private String email;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String currentCity;
    private String currentState;
    private String currentCountry;
    private Double latitude;
    private Double longitude;
    private String profilePhotoUrl;
    private Boolean isPhotoVerified;
    private String tagline;
    private String aboutMe;
    private List<String> languages = new ArrayList<>();
    private Boolean isProfileVerified;
    private Boolean isProfileComplete;
    private Boolean isActive;
    private LocalDateTime lastActiveAt;

    public boolean validate() {
        if (cognitoSub == null) throw new IllegalArgumentException("userId is required.");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("displayName is required.");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("email is required.");
//        if (dateOfBirth == null) throw new IllegalArgumentException("dateOfBirth is required.");
//        if (gender == null) throw new IllegalArgumentException("gender is required.");
        return true;
    }
}
