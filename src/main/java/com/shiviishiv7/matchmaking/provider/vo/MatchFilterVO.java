package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchFilterVO {

    // ── Identity ──────────────────────────────────────────────────────────────
    private Integer id;
    private String cognitoSub;

    // ── Category routing ──────────────────────────────────────────────────────
    private String parentCategory;
    private String childCategory;

    // ── Common filters ────────────────────────────────────────────────────────
    private Integer minAge;
    private Integer maxAge;
    private String preferredGender;
    private String preferredZip;
    private String preferredCity;
    private String preferredState;
    private String preferredCountry;
    private Integer maxTimezoneOffsetHours;
    private Boolean sameCompanyAllowed;

    // ── Matrimonial ───────────────────────────────────────────────────────────
    private String religion;
    private String caste;
    private String subCaste;
    private String gotram;
    private String motherTongue;
    private String dietaryHabits;
    private String highestEducation;
    private String profession;
    private String employmentType;
    private BigDecimal annualIncomeInr;
    private String nativeCity;
    private String nativeState;
    private String familyType;
    private String familyValues;
    private String familyStatus;
    private Integer heightCm;
    private String maritalStatus;
    private String bodyType;
    private String smokingHabit;
    private String drinkingHabit;
    private String manglikStatus;
    private Boolean horoscopeMatchRequired;

    // ── Dating ────────────────────────────────────────────────────────────────
    private String relationshipGoal;
    private String sexualOrientation;
    private String loveLanguage;
    private String personalityType;
    private Boolean hasChildren;
    private Boolean wantsChildren;
    private Integer prefHeightMinCm;

    // ── Fitness ───────────────────────────────────────────────────────────────
    private String fitnessActivities;
    private String fitnessLevel;
    private String workoutDays;
    private String preferredWorkoutTime;
    private String gymName;
    private Boolean isOkWithMixedGender;
    private String sportsLeagueLevel;
    private String fitnessGoal;
    private String dietPreference;

    // ── Flatmate ──────────────────────────────────────────────────────────────
    private String lookingIn;
    private String budgetRangeInr;
    private LocalDate moveInDate;
    private String preferredFlatmateGender;
    private String occupationType;
    private Boolean isVegetarianHousehold;
    private Boolean allowsSmoking;
    private Boolean hasPets;
    private Boolean allowsPets;
    private String sleepSchedule;
    private String cleanlinessLevel;
    private String guestsPolicy;
    private Boolean hasCurrentFlat;

    // ── Gaming ────────────────────────────────────────────────────────────────
    private String platforms;
    private String favoriteGames;
    private String favoriteGenres;
    private String gamingSchedule;
    private String skillLevel;
    private String communicationStyle;
    private Boolean isOkWithNewbies;

    // ── Professional ──────────────────────────────────────────────────────────
    private String currentRole;
    private String currentCompany;
    private Integer yearsOfExperience;
    private String industryDomain;
    private String techStack;
    private String skillsOffering;
    private String skillsSeeking;
    private String mentorshipRole;
    private Boolean openToCoFounder;
    private String preferredCollabMode;

    // ── Travel ────────────────────────────────────────────────────────────────
    private String travelStyle;
    private String preferredDestinations;
    private Integer tripsPerYear;
    private String preferredTripDuration;
    private Boolean hasTraveledAbroad;
    private Integer countriesVisited;
    private String dietaryNeeds;
    private Boolean isOkWithBudgetStays;
    private Boolean isOkWithCamping;
    private String preferredGroupSize;

    public boolean validate() {
        if (cognitoSub == null || cognitoSub.isBlank()) {
            throw new IllegalArgumentException("cognitoSub is required.");
        }
        if (childCategory == null || childCategory.isBlank()) {
            throw new IllegalArgumentException("childCategory is required.");
        }

        try {
            com.shiviishiv7.matchmaking.common.enums.MatchCategory matchedEnum =
                    com.shiviishiv7.matchmaking.common.enums.MatchCategory.valueOf(childCategory.trim().toUpperCase());
            if (parentCategory != null && !parentCategory.isBlank()) {
                if (!matchedEnum.getParentGroup().equalsIgnoreCase(parentCategory.trim())) {
                    throw new IllegalArgumentException("childCategory does not belong to the specified parentCategory.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid childCategory: " + childCategory);
        }

        if (minAge != null && (minAge < 18 || minAge > 99)) {
            throw new IllegalArgumentException("minAge must be between 18 and 99.");
        }
        if (maxAge != null && (maxAge < 18 || maxAge > 99)) {
            throw new IllegalArgumentException("maxAge must be between 18 and 99.");
        }
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("minAge cannot exceed maxAge.");
        }
        if (maxTimezoneOffsetHours != null && (maxTimezoneOffsetHours < 0 || maxTimezoneOffsetHours > 12)) {
            throw new IllegalArgumentException("maxTimezoneOffsetHours must be between 0 and 12.");
        }
        if (preferredZip != null && preferredZip.length() > 20) {
            throw new IllegalArgumentException("preferredZip exceeds maximum length.");
        }
        if (preferredCity != null && preferredCity.length() > 100) {
            throw new IllegalArgumentException("preferredCity exceeds maximum length.");
        }
        if (preferredState != null && preferredState.length() > 100) {
            throw new IllegalArgumentException("preferredState exceeds maximum length.");
        }
        if (preferredCountry != null && preferredCountry.length() > 100) {
            throw new IllegalArgumentException("preferredCountry exceeds maximum length.");
        }

        return true;
    }
}
