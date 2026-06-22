package com.shiviishiv7.matchmaking.processor.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.FitnessExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.FitnessExtProfile;
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
 * Fitness scoring weights (total = 100 pts):
 *
 *   Activities overlap          30 pts  — shared sports/gym/yoga = direct buddy match
 *   Fitness level match         20 pts  — beginner + advanced = frustrating; same = great
 *   Workout schedule overlap    20 pts  — same days + time window = can actually meet
 *   Fitness goal alignment      15 pts  — weight loss + muscle gain = ok; same = better
 *   Same gym                    10 pts  — proximity bonus; same gym = instant meetup
 *   Diet preference match        5 pts  — relevant for post-workout meals together
 */
@Component
@Slf4j
public class FitnessScorer implements CategoryScorer {

    private static final int SCORE_ACTIVITIES = 30;
    private static final int SCORE_LEVEL      = 20;
    private static final int SCORE_SCHEDULE   = 20;
    private static final int SCORE_GOAL       = 15;
    private static final int SCORE_GYM        = 10;
    private static final int SCORE_DIET       = 5;

    @Autowired
    private FitnessExtProfileRepository fitnessRepo;

    @Autowired
    private BaseUserProfileRepository baseProfileRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() {
        return MatchCategory.FITNESS_SPORTS;
    }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        log.trace("Fetching fitness candidates for userId: {}", userId);

        FitnessExtProfile me = fitnessRepo.findByCognitoSub(userId).orElse(null);
        if (me == null) {
            log.warn("Fitness profile missing for userId: {}. Returning empty.", userId);
            return Collections.emptyList();
        }

        List<String> result = fitnessRepo.findAll().stream()
                .filter(c -> !c.getCognitoSub().equals(userId))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSub()))
                // Hard filter: must share at least one activity
                .filter(c -> hasActivityOverlap(me.getFitnessActivities(), c.getFitnessActivities()))
                .map(FitnessExtProfile::getCognitoSub)
                .collect(Collectors.toList());

        log.trace("Fitness hard filter: {} candidates remain for userId: {}", result.size(), userId);
        return result;
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        FitnessExtProfile me        = fitnessRepo.findByCognitoSub(userId).orElseThrow();
        FitnessExtProfile candidate = fitnessRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile candBase    = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        int actScore = scoreActivities(me.getFitnessActivities(), candidate.getFitnessActivities());
        breakdown.put("activities", actScore);
        total += actScore;

        int levelScore = scoreFitnessLevel(me.getFitnessLevel(), candidate.getFitnessLevel());
        breakdown.put("fitnessLevel", levelScore);
        total += levelScore;

        int scheduleScore = scoreSchedule(me.getWorkoutDays(), candidate.getWorkoutDays(),
                me.getPreferredWorkoutTime(), candidate.getPreferredWorkoutTime());
        breakdown.put("schedule", scheduleScore);
        total += scheduleScore;

        int goalScore = scoreGoal(me.getFitnessGoal(), candidate.getFitnessGoal());
        breakdown.put("fitnessGoal", goalScore);
        total += goalScore;

        int gymScore = scoreGym(me.getGymName(), candidate.getGymName());
        breakdown.put("gym", gymScore);
        total += gymScore;

        int dietScore = (StringUtils.isNotEmpty(me.getDietPreference())
                && me.getDietPreference().equalsIgnoreCase(candidate.getDietPreference())) ? SCORE_DIET : 0;
        breakdown.put("diet", dietScore);
        total += dietScore;

        total = Math.min(total, 100);

        MatchCandidateVO vo = new MatchCandidateVO();
        vo.setCognitoSubB(candidateUserId);
        vo.setDisplayName(candBase.getDisplayName());
        vo.setProfilePhotoUrl(candBase.getProfilePhotoUrl());
        vo.setTagline(candBase.getTagline());
        vo.setCurrentCity(candBase.getCurrentCity());
        vo.setCurrentCountry(candBase.getCurrentCountry());
        if (candBase.getDateOfBirth() != null) {
            vo.setAge(Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears());
        }
        vo.setGender(candBase.getGender() != null ? candBase.getGender().name() : null);
        vo.setCompatibilityScore(total);
        vo.setMatchCategory(MatchCategory.FITNESS_SPORTS);
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
        FitnessExtProfile p = fitnessRepo.findByCognitoSub(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getFitnessLevel()))         parts.add(p.getFitnessLevel());
        if (StringUtils.isNotEmpty(p.getFitnessActivities()))    parts.add(p.getFitnessActivities());
        if (StringUtils.isNotEmpty(p.getPreferredWorkoutTime())) parts.add(p.getPreferredWorkoutTime());
        if (StringUtils.isNotEmpty(p.getFitnessGoal()))          parts.add(p.getFitnessGoal());
        return String.join(" · ", parts);
    }

    // ── Scoring helpers ────────────────────────────────────────────────────────

    private boolean hasActivityOverlap(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return false;
        Set<String> mySet    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        mySet.retainAll(theirSet);
        return !mySet.isEmpty();
    }

    private int scoreActivities(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Set<String> mySet    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        mySet.retainAll(theirSet);
        // 1 overlap=10, 2=20, 3+=30
        return Math.min(mySet.size() * 10, SCORE_ACTIVITIES);
    }

    private int scoreFitnessLevel(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Map<String, Integer> levelMap = Map.of(
                "BEGINNER", 1, "INTERMEDIATE", 2, "ADVANCED", 3);
        int myTier    = levelMap.getOrDefault(mine.toUpperCase(), 0);
        int theirTier = levelMap.getOrDefault(theirs.toUpperCase(), 0);
        int diff = Math.abs(myTier - theirTier);
        if (diff == 0) return SCORE_LEVEL;
        if (diff == 1) return SCORE_LEVEL / 2;
        return 0;
    }

    private int scoreSchedule(String myDays, String theirDays, String myTime, String theirTime) {
        int score = 0;
        // Days overlap
        if (StringUtils.isNotEmpty(myDays) && StringUtils.isNotEmpty(theirDays)) {
            Set<String> myDaySet    = new HashSet<>(Arrays.asList(myDays.toUpperCase().split(",")));
            Set<String> theirDaySet = new HashSet<>(Arrays.asList(theirDays.toUpperCase().split(",")));
            myDaySet.retainAll(theirDaySet);
            score += Math.min(myDaySet.size() * 4, 12);
        }
        // Time window match
        if (StringUtils.isNotEmpty(myTime) && myTime.equalsIgnoreCase(theirTime)) score += 8;
        return Math.min(score, SCORE_SCHEDULE);
    }

    private int scoreGoal(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        if (mine.equalsIgnoreCase(theirs)) return SCORE_GOAL;
        // Complementary goals still work (e.g., both want health-related outcomes)
        boolean complementary =
                (mine.contains("Weight") && theirs.contains("Weight")) ||
                (mine.contains("Muscle") && theirs.contains("Endurance")) ||
                (mine.contains("Endurance") && theirs.contains("Muscle"));
        return complementary ? SCORE_GOAL / 2 : 0;
    }

    private int scoreGym(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        return mine.trim().equalsIgnoreCase(theirs.trim()) ? SCORE_GYM : 0;
    }
}
