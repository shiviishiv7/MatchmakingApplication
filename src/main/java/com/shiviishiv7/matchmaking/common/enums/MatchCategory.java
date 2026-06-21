package com.shiviishiv7.matchmaking.common.enums;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum MatchCategory {

    // ── 1. PROFESSIONAL & CAREER GROWTH ──────────────────────────────────────
    EXAM_PREPARATION("Professional & Career Growth", "Exam & Certification Prep"),
    SKILL_SWAPPING("Professional & Career Growth", "Skill Swapping & Co-Learning"),
    MENTORSHIP("Professional & Career Growth", "Leadership & Mentorship"),
    HACKATHON_CREW("Professional & Career Growth", "Side-Hustle & Hackathon Crew"),
    JOB_REFERRAL("Professional & Career Growth", "Job Referral Network"),
    STARTUP_COFOUNDER("Professional & Career Growth", "Co-Founder / Startup Partner"),
    PORTFOLIO_REVIEWER("Professional & Career Growth", "Portfolio & Code Review Buddy"),
    INTERVIEW_PREP("Professional & Career Growth", "Mock Interview Partner"),
    RESEARCH_COLLABORATOR("Professional & Career Growth", "Research & Paper Co-Author"),
    CONTENT_CREATOR_COLLAB("Professional & Career Growth", "Content Creator Collaboration"),
    OPEN_SOURCE_BUDDY("Professional & Career Growth", "Open Source Contribution Buddy"),
    FREELANCE_NETWORK("Professional & Career Growth", "Freelance & Gig Network"),

    // ── 2. SHARED ACTIVITIES & DAILY ROUTINES ────────────────────────────────
    LUNCH_AND_COFFEE("Shared Activities & Daily Routines", "Lunch & Coffee Breaks"),
    MOVIES_AND_SHOWS("Shared Activities & Daily Routines", "Going Out for Movies / Shows"),
    COWORKING_BUDDIES("Shared Activities & Daily Routines", "Co-Working Buddies"),
    FITNESS_SPORTS("Shared Activities & Daily Routines", "Fitness & Sports Partners"),
    GAMING_BUDDIES("Shared Activities & Daily Routines", "Gaming & Online Play"),
    BOOK_READING("Shared Activities & Daily Routines", "Book Club & Reading Circle"),
    COOKING_FOOD("Shared Activities & Daily Routines", "Food & Cooking Enthusiasts"),
    MUSIC_JAMMING("Shared Activities & Daily Routines", "Music Jamming & Band Mates"),
    PHOTOGRAPHY_CLUB("Shared Activities & Daily Routines", "Photography & Reels Club"),
    VOLUNTEERING("Shared Activities & Daily Routines", "Volunteering & Social Causes"),
    BOARD_GAMES("Shared Activities & Daily Routines", "Board Games & Puzzles Night"),
    PET_PLAYDATE("Shared Activities & Daily Routines", "Pet Playdates & Dog Walking"),
    STANDUP_COMEDY("Shared Activities & Daily Routines", "Comedy Shows & Open Mics"),
    SPIRITUAL_GROUP("Shared Activities & Daily Routines", "Meditation & Spiritual Circle"),

    // ── 3. TRAVEL & EXPLORATION ──────────────────────────────────────────────
    TRAVEL_TREKKING("Travel & Exploration", "Travel & Trekking Partners"),
    RIDESHARE("Travel & Exploration", "Daily Commute Ride-Sharing"),
    CITY_EXPLORER("Travel & Exploration", "New in the City Explorer"),
    WEEKEND_GETAWAY("Travel & Exploration", "Weekend Trip Planning"),
    FLATMATE_FINDER("Travel & Exploration", "Flatmate / PG Finder"),
    INTERNATIONAL_RELOCATION("Travel & Exploration", "International Relocation Buddy"),
    ROAD_TRIP("Travel & Exploration", "Road Trip Crew"),
    BACKPACKER("Travel & Exploration", "Budget Backpacker Gang"),

    // ── 4. PERSONAL & LIFESTYLE CONNECTIONS ──────────────────────────────────
    PROFESSIONAL_MATRIMONY("Personal & Lifestyle Connections", "Matrimonial (Verified)"),
    CASUAL_DATING("Personal & Lifestyle Connections", "High-Intent Dating"),
    COMMON_INTERESTS("Personal & Lifestyle Connections", "Common Ground Interests"),
    MENTAL_WELLNESS("Personal & Lifestyle Connections", "Mental Wellness & Support Circle"),
    PARENTING_CIRCLE("Personal & Lifestyle Connections", "Parents & Family Connect"),
    LANGUAGE_EXCHANGE("Personal & Lifestyle Connections", "Language & Culture Exchange"),
    PETS_COMMUNITY("Personal & Lifestyle Connections", "Pet Owners Community"),
    LGBTQ_SAFE_SPACE("Personal & Lifestyle Connections", "LGBTQ+ Safe Space Connect"),
    WOMEN_CIRCLE("Personal & Lifestyle Connections", "Women Empowerment Circle"),
    EXPAT_BUDDY("Personal & Lifestyle Connections", "Expat & NRI Connect"),
    FASHION_STYLE("Personal & Lifestyle Connections", "Fashion & Personal Styling"),
    ASTROLOGY_CIRCLE("Personal & Lifestyle Connections", "Astrology & Numerology Circle"),

    // ── 5. HEALTH & WELLNESS ─────────────────────────────────────────────────
    GYM_PARTNER("Health & Wellness", "Gym & Workout Partner"),
    YOGA_MEDITATION("Health & Wellness", "Yoga & Mindfulness Sessions"),
    DIET_NUTRITION("Health & Wellness", "Diet & Nutrition Accountability"),
    CYCLING_RUNNING("Health & Wellness", "Cycling & Running Club"),
    MENTAL_HEALTH_SUPPORT("Health & Wellness", "Mental Health Peer Support"),
    SOBRIETY_CIRCLE("Health & Wellness", "Sobriety & Clean Living Circle"),
    EARLY_RISERS("Health & Wellness", "Early Risers & Morning Routine");

    // ─────────────────────────────────────────────────────────────────────────

    private final String parentGroup;
    private final String displayName;

    MatchCategory(String parentGroup, String displayName) {
        this.parentGroup = parentGroup;
        this.displayName = displayName;
    }

    public String getParentGroup() {
        return parentGroup;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns all categories belonging to the given parent group (case-insensitive).
     */
    public static List<MatchCategory> getByParent(String parentGroup) {
        return Arrays.stream(values())
                .filter(cat -> cat.getParentGroup().equalsIgnoreCase(parentGroup))
                .collect(Collectors.toList());
    }

    /**
     * Returns all distinct parent group names.
     */
    public static List<String> getAllParentGroups() {
        return Arrays.stream(values())
                .map(MatchCategory::getParentGroup)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Finds a MatchCategory by its enum name (case-insensitive). Returns null if not found.
     */
    public static MatchCategory fromName(String name) {
        return Arrays.stream(values())
                .filter(cat -> cat.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a MatchCategory by its displayName (case-insensitive). Returns null if not found.
     */
    public static MatchCategory fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(cat -> cat.getDisplayName().equalsIgnoreCase(displayName))
                .findFirst()
                .orElse(null);
    }
}
