package com.shiviishiv7.matchmaking.provider.vo.post;

import com.shiviishiv7.matchmaking.common.enums.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartnerPreferenceRequestVO {
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
}
