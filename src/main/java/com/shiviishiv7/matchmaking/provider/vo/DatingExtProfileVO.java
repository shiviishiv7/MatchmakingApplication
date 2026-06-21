package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatingExtProfileVO {

    private Integer id;
    private Integer userId;
    private String dietaryHabits;
    private String smokingHabit;
    private String drinkingHabit;
    private Integer heightCm;
    private String bodyType;
    private String relationshipGoal;
    private String sexualOrientation;
    private Boolean hasChildren;
    private Boolean wantsChildren;
    private String loveLanguage;
    private String personalityType;
    private String interestTags;
    private String promptQuestion1;
    private String promptAnswer1;
    private String promptQuestion2;
    private String promptAnswer2;
    private Integer prefAgeMin;
    private Integer prefAgeMax;
    private String prefGenders;
    private Integer prefHeightMinCm;
    private String prefRelationshipGoal;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        if (prefAgeMin != null && prefAgeMax != null && prefAgeMin > prefAgeMax) throw new IllegalArgumentException("prefAgeMin cannot exceed prefAgeMax.");
        return true;
    }
}
