package com.shiviishiv7.matchmaking.processor.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.FlatmateExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.FlatmateExtProfile;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flatmate scoring weights (total = 100 pts):
 *
 *   Location area match         25 pts  — must be looking in overlapping areas (hard filter too)
 *   Budget range overlap        20 pts  — non-overlapping budgets = non-starter
 *   Lifestyle compatibility     20 pts  — sleep, cleanliness, guests policy
 *   Diet / smoking / pets       15 pts  — household dealbreakers
 *   Occupation type match       10 pts  — working professional + student = schedule clash risk
 *   Move-in date proximity      10 pts  — within 2 weeks = full; within a month = partial
 */
@Component
@Slf4j
public class FlatmateScorer implements CategoryScorer {

    private static final int SCORE_LOCATION    = 25;
    private static final int SCORE_BUDGET      = 20;
    private static final int SCORE_LIFESTYLE   = 20;
    private static final int SCORE_HOUSEHOLD   = 15;
    private static final int SCORE_OCCUPATION  = 10;
    private static final int SCORE_MOVE_IN     = 10;

    @Autowired
    private FlatmateExtProfileRepository flatmateRepo;

    @Autowired
    private BaseUserProfileRepository baseProfileRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() {
        return MatchCategory.FLATMATE_FINDER;
    }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        log.trace("Fetching flatmate candidates for userId: {}", userId);

        FlatmateExtProfile me = flatmateRepo.findByCognitoSub(userId).orElse(null);
        if (me == null) {
            log.warn("Flatmate profile missing for userId: {}. Returning empty.", userId);
            return Collections.emptyList();
        }

        List<String> result = flatmateRepo.findAll().stream()
                .filter(c -> !c.getCognitoSub().equals(userId))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSub()))
                // Hard filter 1: must be looking in the same city/area
                .filter(c -> isLocationCompatible(me.getLookingIn(), c.getLookingIn()))
                // Hard filter 2: budgets must overlap (my max >= their min AND my min <= their max)
                .filter(c -> isBudgetCompatible(me.getBudgetRangeInr(), c.getBudgetRangeInr()))
                // Hard filter 3: vegetarian household — non-negotiable for many users
                .filter(c -> isVegCompatible(me.getIsVegetarianHousehold(), c.getIsVegetarianHousehold()))
                .map(FlatmateExtProfile::getCognitoSub)
                .collect(Collectors.toList());

        log.trace("Flatmate hard filter: {} candidates remain for userId: {}", result.size(), userId);
        return result;
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        FlatmateExtProfile me        = flatmateRepo.findByCognitoSub(userId).orElseThrow();
        FlatmateExtProfile candidate = flatmateRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile candBase     = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        // Location
        int locationScore = scoreLocation(me.getLookingIn(), candidate.getLookingIn());
        breakdown.put("location", locationScore);
        total += locationScore;

        // Budget
        int budgetScore = scoreBudget(me.getBudgetRangeInr(), candidate.getBudgetRangeInr());
        breakdown.put("budget", budgetScore);
        total += budgetScore;

        // Lifestyle: sleep schedule + cleanliness + guests policy
        int lifestyleScore = scoreLifestyle(me, candidate);
        breakdown.put("lifestyle", lifestyleScore);
        total += lifestyleScore;

        // Household rules: diet + smoking + pets
        int householdScore = scoreHousehold(me, candidate);
        breakdown.put("household", householdScore);
        total += householdScore;

        // Occupation type
        int occupationScore = scoreOccupation(me.getOccupationType(), candidate.getOccupationType());
        breakdown.put("occupation", occupationScore);
        total += occupationScore;

        // Move-in date
        int moveInScore = scoreMoveInDate(me.getMoveInDate(), candidate.getMoveInDate());
        breakdown.put("moveInDate", moveInScore);
        total += moveInScore;

        total = Math.min(total, 100);

        MatchCandidateVO vo = new MatchCandidateVO();
        vo.setCognitoSubB(candidateUserId);
        vo.setName(candBase.getName());
        vo.setProfilePhotoUrl(candBase.getProfilePhotoUrl());
        vo.setTagline(candBase.getTagline());
        vo.setCurrentCity(candBase.getCurrentCity());
        vo.setCurrentCountry(candBase.getCurrentCountry());
        if (candBase.getDateOfBirth() != null) {
            vo.setAge(Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears());
        }
        vo.setGender(candBase.getGender() != null ? candBase.getGender().name() : null);
        vo.setCompatibilityScore(total);
        vo.setMatchCategory(MatchCategory.FLATMATE_FINDER);
        vo.setCategorySnippet(buildSnippet(candidateUserId));

        try {
            vo.setScoreBreakdown(objectMapper.writeValueAsString(breakdown));
        } catch (Exception e) {
            log.warn("Could not serialize score breakdown for candidateUserId: {}", candidateUserId);
        }

        return vo;
    }

    @Override
    public String buildSnippet(String candidateUserId) {
        FlatmateExtProfile p = flatmateRepo.findByCognitoSub(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getLookingIn()))      parts.add(p.getLookingIn());
        if (StringUtils.isNotEmpty(p.getBudgetRangeInr())) parts.add("₹" + p.getBudgetRangeInr());
        if (StringUtils.isNotEmpty(p.getSleepSchedule()))  parts.add(p.getSleepSchedule());
        if (p.getMoveInDate() != null)                     parts.add("From " + p.getMoveInDate());
        return String.join(" · ", parts);
    }

    // ── Hard filter helpers ────────────────────────────────────────────────────

    private boolean isLocationCompatible(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return false;
        // Check if any area tokens overlap (comma-separated city areas)
        Set<String> myAreas    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirAreas = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        myAreas.retainAll(theirAreas);
        return !myAreas.isEmpty();
    }

    private boolean isBudgetCompatible(String mine, String theirs) {
        // Format: "10000-15000"
        try {
            int[] myRange    = parseBudget(mine);
            int[] theirRange = parseBudget(theirs);
            if (myRange == null || theirRange == null) return true; // skip if not set
            // Overlap: my max >= their min AND my min <= their max
            return myRange[1] >= theirRange[0] && myRange[0] <= theirRange[1];
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isVegCompatible(Boolean mine, Boolean theirs) {
        // If either insists on vegetarian household, both must agree
        if (Boolean.TRUE.equals(mine) && Boolean.FALSE.equals(theirs)) return false;
        if (Boolean.TRUE.equals(theirs) && Boolean.FALSE.equals(mine)) return false;
        return true;
    }

    // ── Scoring helpers ────────────────────────────────────────────────────────

    private int scoreLocation(String mine, String theirs) {
        if (!isLocationCompatible(mine, theirs)) return 0;
        Set<String> myAreas    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirAreas = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        myAreas.retainAll(theirAreas);
        return Math.min(myAreas.size() * 12, SCORE_LOCATION);
    }

    private int scoreBudget(String mine, String theirs) {
        try {
            int[] myRange    = parseBudget(mine);
            int[] theirRange = parseBudget(theirs);
            if (myRange == null || theirRange == null) return 0;
            // Calculate overlap amount as % of the smaller range
            int overlapMin = Math.max(myRange[0], theirRange[0]);
            int overlapMax = Math.min(myRange[1], theirRange[1]);
            if (overlapMax < overlapMin) return 0;
            int overlap    = overlapMax - overlapMin;
            int myRange_sz = myRange[1] - myRange[0];
            if (myRange_sz == 0) return SCORE_BUDGET;
            double ratio = (double) overlap / myRange_sz;
            return (int) Math.min(ratio * SCORE_BUDGET, SCORE_BUDGET);
        } catch (Exception e) {
            return 0;
        }
    }

    private int scoreLifestyle(FlatmateExtProfile me, FlatmateExtProfile candidate) {
        int score = 0;
        // Sleep schedule: Early Bird / Night Owl / Flexible
        if (StringUtils.isNotEmpty(me.getSleepSchedule())) {
            if (me.getSleepSchedule().equalsIgnoreCase(candidate.getSleepSchedule())) score += 8;
            else if ("Flexible".equalsIgnoreCase(me.getSleepSchedule())
                    || "Flexible".equalsIgnoreCase(candidate.getSleepSchedule())) score += 4;
        }
        // Cleanliness
        if (StringUtils.isNotEmpty(me.getCleanlinessLevel())
                && me.getCleanlinessLevel().equalsIgnoreCase(candidate.getCleanlinessLevel())) score += 7;
        // Guests policy
        if (StringUtils.isNotEmpty(me.getGuestsPolicy())
                && me.getGuestsPolicy().equalsIgnoreCase(candidate.getGuestsPolicy())) score += 5;
        return Math.min(score, SCORE_LIFESTYLE);
    }

    private int scoreHousehold(FlatmateExtProfile me, FlatmateExtProfile candidate) {
        int score = 0;
        // Diet/veg already hard-filtered, award points for agreement
        if (me.getIsVegetarianHousehold() != null
                && me.getIsVegetarianHousehold().equals(candidate.getIsVegetarianHousehold())) score += 5;
        // Smoking
        if (me.getAllowsSmoking() != null
                && me.getAllowsSmoking().equals(candidate.getAllowsSmoking())) score += 5;
        // Pets
        if (me.getHasPets() != null && me.getAllowsPets() != null) {
            // I have pets → candidate must allow pets
            if (Boolean.TRUE.equals(me.getHasPets()) && Boolean.TRUE.equals(candidate.getAllowsPets())) score += 5;
            // I don't have pets and don't want them → candidate also has no pets
            if (Boolean.FALSE.equals(me.getHasPets()) && Boolean.FALSE.equals(candidate.getHasPets())) score += 5;
        }
        return Math.min(score, SCORE_HOUSEHOLD);
    }

    private int scoreOccupation(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        if (mine.equalsIgnoreCase(theirs)) return SCORE_OCCUPATION;
        if ("Any".equalsIgnoreCase(mine) || "Any".equalsIgnoreCase(theirs))
            return SCORE_OCCUPATION / 2;
        return 0;
    }

    private int scoreMoveInDate(LocalDate mine, LocalDate theirs) {
        if (mine == null || theirs == null) return SCORE_MOVE_IN / 2;
        long days = Math.abs(mine.toEpochDay() - theirs.toEpochDay());
        if (days <= 14) return SCORE_MOVE_IN;
        if (days <= 30) return SCORE_MOVE_IN / 2;
        return 0;
    }

    private int[] parseBudget(String range) {
        if (StringUtils.isEmpty(range)) return null;
        String[] parts = range.split("-");
        if (parts.length != 2) return null;
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }
}
