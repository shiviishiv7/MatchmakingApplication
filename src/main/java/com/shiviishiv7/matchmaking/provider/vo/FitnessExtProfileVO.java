package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FitnessExtProfileVO {

    private Integer id;
    private Integer userId;
    private String fitnessActivities;
    private String fitnessLevel;
    private String workoutDays;
    private String preferredWorkoutTime;
    private String gymName;
    private Boolean isOkWithMixedGender;
    private String sportsLeagueLevel;
    private String fitnessGoal;
    private String dietPreference;

    public boolean validate() {
        if (userId == null) throw new IllegalArgumentException("userId is required.");
        return true;
    }
}
