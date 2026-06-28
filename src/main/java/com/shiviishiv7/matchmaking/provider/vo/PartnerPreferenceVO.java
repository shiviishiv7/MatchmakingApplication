package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.*;
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
    private Gender genderPref;

    private MaritalStatus maritalStatusPref;
    private String preferredStates;
    private Boolean openToRelocation;

    private Religion religionPref;
    private Language motherTonguePref;
    private DietPreference dietaryPref;
    private Qualification educationPref;
    private Profession employmentTypePref;
    private BigDecimal incomeMinInr;
    private BigDecimal incomeMaxInr;

    private SmokingHabit smokingPref;
    private DrinkingHabit drinkingPref;
    private FamilyType familyTypePref;
    private FamilyValues familyValuesPref;

    private Boolean wantsChildrenPref;
    private MarriageTimeline marriageTimelinePref;
    private Boolean okWithPartnerWorkingPref;
    private RelationshipGoal relationshipGoalPref;

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
