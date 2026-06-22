package com.shiviishiv7.matchmaking.common.enums;



import java.util.Arrays;

public enum ProfessionalCategory {

    EXAM_PREPARATION("Exam & Certification Prep"),
    SKILL_SWAPPING("Skill Swapping & Co-Learning"),
    MENTORSHIP("Leadership & Mentorship"),
    HACKATHON_CREW("Side-Hustle & Hackathon Crew"),
    JOB_REFERRAL("Job Referral Network"),
    STARTUP_COFOUNDER("Co-Founder / Startup Partner"),
    PORTFOLIO_REVIEWER("Portfolio & Code Review Buddy"),
    INTERVIEW_PREP("Mock Interview Partner"),
    RESEARCH_COLLABORATOR("Research & Paper Co-Author"),
    CONTENT_CREATOR_COLLAB("Content Creator Collaboration"),
    OPEN_SOURCE_BUDDY("Open Source Contribution Buddy"),
    FREELANCE_NETWORK("Freelance & Gig Network");

    private final String displayName;

    // Hardcoded track label to match your cascading parent layout context
    public static final String PARENT_GROUP = "Professional & Career Growth";

    ProfessionalCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getParentGroup() {
        return PARENT_GROUP;
    }

    /**
     * Finds a ProfessionalCategory by its exact enum name (case-insensitive).
     * Returns null if not found.
     */
    public static ProfessionalCategory fromName(String name) {
        if (name == null || name.isBlank()) return null;
        return Arrays.stream(values())
                .filter(cat -> cat.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a ProfessionalCategory by its clear UI displayName description (case-insensitive).
     * Returns null if not found.
     */
    public static ProfessionalCategory fromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return null;
        return Arrays.stream(values())
                .filter(cat -> cat.getDisplayName().equalsIgnoreCase(displayName.trim()))
                .findFirst()
                .orElse(null);
    }
}
