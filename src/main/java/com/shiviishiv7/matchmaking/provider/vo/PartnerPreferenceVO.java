package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.IntentType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerPreferenceVO {

    private Integer id;
    private Long postId;
    private String cognitoSub;
    private IntentType intent;

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

    public boolean validate() {
        if (ageMin != null && ageMax != null && ageMin > ageMax)
            throw new IllegalArgumentException("ageMin cannot exceed ageMax.");
        if (heightMinCm != null && heightMaxCm != null && heightMinCm > heightMaxCm)
            throw new IllegalArgumentException("heightMinCm cannot exceed heightMaxCm.");
        if (incomeMinInr != null && incomeMaxInr != null && incomeMinInr.compareTo(incomeMaxInr) > 0)
            throw new IllegalArgumentException("incomeMinInr cannot exceed incomeMaxInr.");
        return true;
    }
}
