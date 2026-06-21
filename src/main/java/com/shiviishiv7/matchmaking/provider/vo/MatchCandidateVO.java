package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The object returned to the frontend for each match suggestion.
 * Combines the candidate's base profile + category-specific snippet
 * + the computed compatibility score for this specific user pair.
 */
@Getter @Setter @ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchCandidateVO {

    private String cognitoSubB;
    private String displayName;
    private String profilePhotoUrl;
    private String tagline;
    private String currentCity;
    private String currentCountry;
    private int age;
    private String gender;

    private int compatibilityScore;        // 0-100
    private String scoreBreakdown;         // JSON string for frontend display

    private MatchCategory matchCategory;

    // Category-specific preview snippet shown on discovery card
    // Populated by each category scorer — only a few key fields, not the full extension
    private String categorySnippet;        // e.g. "Vegetarian · MBA · 12 LPA · Mumbai"
}
