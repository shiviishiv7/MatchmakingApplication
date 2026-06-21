package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatrimonialExtProfileVO {

    private Integer id;
    private Integer userId;
    private String religion;
    private String caste;
    private String subCaste;
    private String gotram;
    private String motherTongue;
    private String dietaryHabits;
    private String highestEducation;
    private String educationDetail;
    private String profession;
    private String employerName;
    private String employmentType;
    private BigDecimal annualIncomeInr;
    private String nativeCity;
    private String nativeState;
    private String familyType;
    private String familyValues;
    private String familyStatus;
    private String fatherOccupation;
    private String motherOccupation;
    private Integer siblingsCount;
    private String siblingsDetail;
    private Integer heightCm;
    private String maritalStatus;
    private String bodyType;
    private String complexion;
    private String smokingHabit;
    private String drinkingHabit;
    private String manglikStatus;
    private String rashi;
    private String nakshatra;
    private String birthPlace;
    private LocalTime birthTime;
    private Boolean horoscopeMatchRequired;
    private PartnerPreferenceVO partnerPreferenceVO;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        return true;
    }
}
