package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.model.User;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.Period;
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
        // 1. Email Validations
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty.");
        }
        if (firstName.trim().length() > 80) {
            throw new IllegalArgumentException("First name cannot exceed 80 characters.");
        }
        if (lastName != null && lastName.trim().length() > 80) {
            throw new IllegalArgumentException("Last name cannot exceed 80 characters.");
        }


        if (companyId == null || companyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Company ID cannot be null or empty.");
        }
        if (cognitoSub == null || cognitoSub.trim().isEmpty()) {
            throw new IllegalArgumentException("Cognito Subject association identity string (cognitoSub) is required.");
        }

        // 4. Profile Demographics Attributes
        if (gender == null) {
            throw new IllegalArgumentException("Gender alignment parameter is required.");
        }

        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be a future date.");
        }

        // Dynamic integrity check: Cross-verify if input age property aligns with calculated DOB age
        int calculatedAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (age != calculatedAge) {
            // Auto-correct or flag mismatch depending on preference; here we enforce validation mapping integrity
            throw new IllegalArgumentException("Provided age does not structurally align with Date of Birth data.");
        }
        if (calculatedAge < 18) {
            throw new IllegalArgumentException("Users must be at least 18 years old to join corporate match pools.");
        }
        
        // 6. Deep Cascade Validation
        if (addressVO != null) {
            // Triggers nested verification check implemented inside AddressVO
            addressVO.validate();
        }

        return true;
    }
}
