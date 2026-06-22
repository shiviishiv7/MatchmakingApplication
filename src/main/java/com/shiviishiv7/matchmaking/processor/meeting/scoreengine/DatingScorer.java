package com.shiviishiv7.matchmaking.processor.meeting.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.DatingExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.DatingExtProfile;
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
 * Dating scoring weights (total = 100 pts):
 *
 *   Relationship goal match     25 pts  — Serious/Casual mismatch is a hard dealbreaker
 *   Interest tags overlap       20 pts  — shared hobbies drive engagement
 *   Age preference match        15 pts  — within preferred range = full, near boundary = partial
 *   Lifestyle compatibility     15 pts  — diet + smoking + drinking alignment
 *   Height preference match     10 pts  — only scored if candidate has set pref
 *   Personality type match      10 pts  — MBTI/Big5 complement scoring
 *   Mutual preference boost      5 pts  — candidate's pref also accepts me
 */
@Component
@Slf4j
public class DatingScorer implements CategoryScorer {

    private static final int SCORE_RELATIONSHIP_GOAL = 25;
    private static final int SCORE_INTERESTS         = 20;
    private static final int SCORE_AGE_PREF          = 15;
    private static final int SCORE_LIFESTYLE         = 15;
    private static final int SCORE_HEIGHT            = 10;
    private static final int SCORE_PERSONALITY       = 10;
    private static final int SCORE_MUTUAL_BOOST      = 5;

    @Autowired
    private DatingExtProfileRepository datingRepo;

    @Autowired
    private BaseUserProfileRepository baseProfileRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() {
        return MatchCategory.CASUAL_DATING;
    }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        log.trace("Fetching dating candidates for userId: {}", userId);

        DatingExtProfile me    = datingRepo.findByCognitoSub(userId).orElse(null);
        BaseUserProfile myBase = baseProfileRepo.findByCognitoSub(userId).orElse(null);
        if (me == null || myBase == null) {
            log.warn("Dating or base profile missing for userId: {}. Returning empty.", userId);
            return Collections.emptyList();
        }

        String myGender = myBase.getGender() != null ? myBase.getGender().name() : null;
        int myAge       = myBase.getDateOfBirth() != null
                ? Period.between(myBase.getDateOfBirth(), LocalDate.now()).getYears() : 0;

        List<String> result = new ArrayList<>();

        List<DatingExtProfile> pool = datingRepo.findAll().stream()
                .filter(c -> !c.getCognitoSub().equals(userId))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSub()))
                .collect(Collectors.toList());

        for (DatingExtProfile candidate : pool) {
            BaseUserProfile candBase = baseProfileRepo.findByCognitoSub(candidate.getCognitoSub()).orElse(null);
            if (candBase == null) continue;

            // Hard filter 1: gender preference
            if (StringUtils.isNotEmpty(me.getPrefGenders())) {
                String candGender = candBase.getGender() != null ? candBase.getGender().name() : "";
                if (!me.getPrefGenders().contains(candGender)) continue;
            }

            // Hard filter 2: my age must be within candidate's preferred range
            if (candidate.getPrefAgeMin() != null && myAge < candidate.getPrefAgeMin()) continue;
            if (candidate.getPrefAgeMax() != null && myAge > candidate.getPrefAgeMax()) continue;

            // Hard filter 3: candidate age must be within my preferred range
            if (candBase.getDateOfBirth() != null) {
                int candAge = Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears();
                if (me.getPrefAgeMin() != null && candAge < me.getPrefAgeMin()) continue;
                if (me.getPrefAgeMax() != null && candAge > me.getPrefAgeMax()) continue;
            }

            result.add(candidate.getCognitoSub());
        }

        log.trace("Dating hard filter: {} candidates remain for userId: {}", result.size(), userId);
        return result;
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        DatingExtProfile me        = datingRepo.findByCognitoSub(userId).orElseThrow();
        DatingExtProfile candidate = datingRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile candBase   = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile myBase     = baseProfileRepo.findByCognitoSub(userId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        // Relationship goal
        int goalScore = scoreRelationshipGoal(me.getRelationshipGoal(), candidate.getRelationshipGoal());
        breakdown.put("relationshipGoal", goalScore);
        total += goalScore;

        // Interest tags overlap
        int interestScore = scoreInterests(me.getInterestTags(), candidate.getInterestTags());
        breakdown.put("interests", interestScore);
        total += interestScore;

        // Age preference
        int ageScore = scoreAgePref(me, candBase);
        breakdown.put("agePref", ageScore);
        total += ageScore;

        // Lifestyle (diet + smoking + drinking)
        int lifestyleScore = scoreLifestyle(me, candidate);
        breakdown.put("lifestyle", lifestyleScore);
        total += lifestyleScore;

        // Height preference
        int heightScore = scoreHeight(me.getPrefHeightMinCm(), candidate.getHeightCm());
        breakdown.put("height", heightScore);
        total += heightScore;

        // Personality type
        int personalityScore = scorePersonality(me.getPersonalityType(), candidate.getPersonalityType());
        breakdown.put("personality", personalityScore);
        total += personalityScore;

        // Mutual boost — does candidate's pref also accept me?
        if (isMutualMatch(candidate, myBase)) {
            breakdown.put("mutualBoost", SCORE_MUTUAL_BOOST);
            total += SCORE_MUTUAL_BOOST;
        }

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
        vo.setMatchCategory(MatchCategory.CASUAL_DATING);
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
        DatingExtProfile p = datingRepo.findByCognitoSub(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getRelationshipGoal())) parts.add(p.getRelationshipGoal());
        if (StringUtils.isNotEmpty(p.getDietaryHabits()))    parts.add(p.getDietaryHabits());
        if (StringUtils.isNotEmpty(p.getLoveLanguage()))     parts.add(p.getLoveLanguage());
        if (StringUtils.isNotEmpty(p.getPersonalityType()))  parts.add(p.getPersonalityType());
        return String.join(" · ", parts);
    }

    // ── Scoring helpers ────────────────────────────────────────────────────────

    private int scoreRelationshipGoal(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        // Exact match = full; "Open to both" = partial
        if (mine.equalsIgnoreCase(theirs)) return SCORE_RELATIONSHIP_GOAL;
        if ("Open to both".equalsIgnoreCase(mine) || "Open to both".equalsIgnoreCase(theirs))
            return SCORE_RELATIONSHIP_GOAL / 2;
        return 0;
    }

    private int scoreInterests(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Set<String> mySet    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        mySet.retainAll(theirSet);
        if (mySet.isEmpty()) return 0;
        // 1 overlap=5, 2=10, 3=15, 4+=20
        return Math.min(mySet.size() * 5, SCORE_INTERESTS);
    }

    private int scoreAgePref(DatingExtProfile me, BaseUserProfile candBase) {
        if (candBase.getDateOfBirth() == null) return 0;
        int candAge = Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears();
        boolean withinMin = me.getPrefAgeMin() == null || candAge >= me.getPrefAgeMin();
        boolean withinMax = me.getPrefAgeMax() == null || candAge <= me.getPrefAgeMax();
        if (withinMin && withinMax) return SCORE_AGE_PREF;
        // Within 2 years of boundary = partial
        boolean nearMin = me.getPrefAgeMin() != null && candAge >= me.getPrefAgeMin() - 2;
        boolean nearMax = me.getPrefAgeMax() != null && candAge <= me.getPrefAgeMax() + 2;
        if (nearMin && nearMax) return SCORE_AGE_PREF / 2;
        return 0;
    }

    private int scoreLifestyle(DatingExtProfile me, DatingExtProfile candidate) {
        int score = 0;
        if (StringUtils.isNotEmpty(me.getDietaryHabits())
                && me.getDietaryHabits().equalsIgnoreCase(candidate.getDietaryHabits())) score += 5;
        if (StringUtils.isNotEmpty(me.getSmokingHabit())
                && me.getSmokingHabit().equalsIgnoreCase(candidate.getSmokingHabit()))   score += 5;
        if (StringUtils.isNotEmpty(me.getDrinkingHabit())
                && me.getDrinkingHabit().equalsIgnoreCase(candidate.getDrinkingHabit())) score += 5;
        return Math.min(score, SCORE_LIFESTYLE);
    }

    private int scoreHeight(Integer myMinPref, Integer candHeight) {
        if (myMinPref == null || candHeight == null) return SCORE_HEIGHT; // no pref = full score
        return candHeight >= myMinPref ? SCORE_HEIGHT : 0;
    }

    private int scorePersonality(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        if (mine.equalsIgnoreCase(theirs)) return SCORE_PERSONALITY;
        // MBTI complements: IN/ES types tend to complement each other
        boolean complement = isComplementaryMBTI(mine, theirs);
        return complement ? SCORE_PERSONALITY / 2 : 0;
    }

    private boolean isComplementaryMBTI(String a, String b) {
        if (a.length() < 2 || b.length() < 2) return false;
        // I/E complement
        char aIE = a.charAt(0), bIE = b.charAt(0);
        return (aIE == 'I' && bIE == 'E') || (aIE == 'E' && bIE == 'I');
    }

    private boolean isMutualMatch(DatingExtProfile candidate, BaseUserProfile myBase) {
        if (myBase.getDateOfBirth() == null) return false;
        int myAge = Period.between(myBase.getDateOfBirth(), LocalDate.now()).getYears();
        if (candidate.getPrefAgeMin() != null && myAge < candidate.getPrefAgeMin()) return false;
        if (candidate.getPrefAgeMax() != null && myAge > candidate.getPrefAgeMax()) return false;
        if (StringUtils.isNotEmpty(candidate.getPrefGenders())
                && myBase.getGender() != null
                && !candidate.getPrefGenders().contains(myBase.getGender().name())) return false;
        return true;
    }
}
