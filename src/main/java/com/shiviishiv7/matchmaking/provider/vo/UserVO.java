package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.model.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVO {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private UUID companyId;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String timezone;
    private String industry;
    private String bio;
    private String profilePictureUrl;
    private UserStatus status;
    private Boolean isActive;
//    private List<String> interests;

    public boolean validate() {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (firstName == null || firstName.isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }
        return true;
    }

    public User fromVO() {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGender(gender);
        user.setDateOfBirth(dateOfBirth);
        user.setTimezone(timezone);
        user.setIndustry(industry);
        user.setBio(bio);
        user.setProfilePictureUrl(profilePictureUrl);
        user.setStatus(status);
        user.setIsActive(isActive);
//        user.setInterests(interests);
        return user;
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setFirstName(user.getFirstName());
        vo.setLastName(user.getLastName());
//        vo.setCompanyId(user.getCompany() != null ? user.getCompany().getId() : null);
        vo.setGender(user.getGender());
        vo.setDateOfBirth(user.getDateOfBirth());
        vo.setTimezone(user.getTimezone());
        vo.setIndustry(user.getIndustry());
        vo.setBio(user.getBio());
        vo.setProfilePictureUrl(user.getProfilePictureUrl());
        vo.setStatus(user.getStatus());
        vo.setIsActive(user.getIsActive());
//        vo.setInterests(user.getInterests());
        return vo;
    }
}
