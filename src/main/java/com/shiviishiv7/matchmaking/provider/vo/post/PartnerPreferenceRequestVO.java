package com.shiviishiv7.matchmaking.provider.vo.post;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartnerPreferenceRequestVO {
    private Integer ageMin;
    private Integer ageMax;
    private Integer heightMinCm;
    private Integer heightMaxCm;
    private String genderPref;
    private String maritalStatusPref;
    private String preferredStates;
    private Boolean openToRelocation;
    private String religionPref;
    private String motherTonguePref;
    private String dietaryPref;
    private String educationPref;
    private String employmentTypePref;
    private BigDecimal incomeMinInr;
    private BigDecimal incomeMaxInr;
    private String smokingPref;
    private String drinkingPref;
    private String familyTypePref;
    private String familyValuesPref;
    private Boolean wantsChildrenPref;
    private String marriageTimelinePref;
    private Boolean okWithPartnerWorkingPref;
    private String relationshipGoalPref;
    private String aboutPartner;
}
