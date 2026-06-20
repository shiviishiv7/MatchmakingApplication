package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.model.User;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserVO {

    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String companyId;
    private Gender gender;
    private LocalDate dateOfBirth;
    private AddressVO addressVO;
//    private String timezone;
//    private String industry;
//    private String bio;
//    private String profilePictureUrl;
    private UserStatus status;
    private Boolean isActive;
    private String cognitoSub;
    private int age;
//    private List<String> interests;

    public boolean validate() {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (firstName == null || firstName.isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
//        if (companyId == null) {
//            throw new IllegalArgumentException("Company ID cannot be null");
//        }
        return true;
    }
}
