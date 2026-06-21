package com.shiviishiv7.matchmaking.provider.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionalExtProfileVO {

    private Integer id;
    private String cognitoSub;
    private String currentRole;
    private String currentCompany;
    private Integer yearsOfExperience;
    private String industryDomain;
    private String techStack;
    private String skillsOffering;
    private String skillsSeeking;
    private String mentorshipRole;
    private Boolean openToCoFounder;
    private String startupIdeas;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String certifications;
    private String preferredCollabMode;
    private String availabilitySlots;

    public boolean validate() {
        if (cognitoSub == null) throw new IllegalArgumentException("userId is required.");
        return true;
    }
}
