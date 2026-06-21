package com.shiviishiv7.matchmaking.processor.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;

import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatrimonialExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.MatrimonialExtProfile;
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
 * Matrimonial scoring weights (total = 100 pts):
 *
 *   Religion match          20 pts  — hard dealbreaker for most families
 *   Caste / sub-caste       15 pts  — only scored if user has set caste pref
 *   Mother tongue           10 pts  — language compatibility
 *   Diet compatibility      10 pts  — vegetarian families often filter hard on this
 *   Education level         10 pts  — graduate vs postgraduate tier match
 *   Income range overlap    10 pts  — within 1 bracket = full, 2 brackets = half
 *   Location match          10 pts  — same city=10, same state=6, same country=3
 *   Manglik compatibility    5 pts  — only scored if both have preference set
 *   Mutual preference boost 10 pts  — added in Phase 3 if candidate also prefers you
 */
@Component
@Slf4j
public class MatrimonialScorer implements CategoryScorer {

    private static final int SCORE_RELIGION      = 20;
    private static final int SCORE_CASTE         = 15;
    private static final int SCORE_MOTHER_TONGUE = 10;
    private static final int SCORE_DIET          = 10;
    private static final int SCORE_EDUCATION     = 10;
    private static final int SCORE_INCOME        = 10;
    private static final int SCORE_LOCATION      = 10;
    private static final int SCORE_MANGLIK       = 5;
    private static final int SCORE_MUTUAL_BOOST  = 10;

    @Autowired
    private MatrimonialExtProfileRepository matrimonialRepo;

    @Autowired
    private BaseUserProfileRepository baseProfileRepo;

    @Autowired
    private CategoryProfileRegistryRepository registryRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() {
        return MatchCategory.PROFESSIONAL_MATRIMONY;
    }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        log.trace("Fetching matrimonial candidates for userId: {}", userId);

        MatrimonialExtProfile me = matrimonialRepo.findByCognitoSubB(userId).orElse(null);
        BaseUserProfile myBase  = baseProfileRepo.findByCognitoSub(userId).orElse(null);
        if (me == null || myBase == null) {
            log.warn("Matrimonial or base profile missing for userId: {}. Returning empty.", userId);
            return Collections.emptyList();
        }

        PartnerPreference pref = me.getPartnerPreference();

        // Derive my age for reverse-preference check
        int myAge = Period.between(myBase.getDateOfBirth(), LocalDate.now()).getYears();
        String myGender = myBase.getGender() != null ? myBase.getGender().name() : null;

        // Fetch all active matrimonial users except excluded
        List<MatrimonialExtProfile> pool = matrimonialRepo.findAll().stream()
            .filter(c -> !c.getCognitoSubB().equals(userId))
            .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSubB()))
            .collect(Collectors.toList());

        List<String> result = new ArrayList<>();
        for (MatrimonialExtProfile candidate : pool) {
            BaseUserProfile candBase = baseProfileRepo.findByCognitoSub(candidate.getCognitoSubB()).orElse(null);
            if (candBase == null) continue;

            // Hard filter 1: gender preference
            if (pref != null && StringUtils.isNotEmpty(pref.getMaritalStatusPref())) {
                String candGender = candBase.getGender() != null ? candBase.getGender().name() : "";
                // Simple opposite-gender check (can be extended for other orientations)
                if (myGender != null && myGender.equals(candGender)) continue;
            }

            // Hard filter 2: age range
            if (candBase.getDateOfBirth() != null) {
                int candAge = Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears();
                if (pref != null) {
                    if (pref.getAgeMin() != null && candAge < pref.getAgeMin()) continue;
                    if (pref.getAgeMax() != null && candAge > pref.getAgeMax()) continue;
                }
            }

            // Hard filter 3: religion (if user has set religion preference)
            if (pref != null && StringUtils.isNotEmpty(pref.getReligionPref())) {
                if (!pref.getReligionPref().contains(candidate.getReligion())) continue;
            }

            result.add(candidate.getCognitoSubB());
        }

        log.trace("Matrimonial hard filter: {} candidates remain for userId: {}", result.size(), userId);
        return result;
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        MatrimonialExtProfile me        = matrimonialRepo.findByCognitoSubB(userId).orElseThrow();
        MatrimonialExtProfile candidate = matrimonialRepo.findByCognitoSubB(candidateUserId).orElseThrow();
        BaseUserProfile candBase        = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();

        PartnerPreference myPref   = me.getPartnerPreference();
        PartnerPreference candPref = candidate.getPartnerPreference();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        // Religion
        int religionScore = scoreReligion(myPref, candidate.getReligion());
        breakdown.put("religion", religionScore);
        total += religionScore;

        // Caste
        int casteScore = scoreCaste(myPref, candidate.getCaste(), candidate.getGotram(), me.getGotram());
        breakdown.put("caste", casteScore);
        total += casteScore;

        // Mother tongue
        int tongueScore = scoreMotherTongue(me.getMotherTongue(), candidate.getMotherTongue());
        breakdown.put("motherTongue", tongueScore);
        total += tongueScore;

        // Diet
        int dietScore = scoreDiet(myPref, candidate.getDietaryHabits());
        breakdown.put("diet", dietScore);
        total += dietScore;

        // Education
        int eduScore = scoreEducation(me.getHighestEducation(), candidate.getHighestEducation());
        breakdown.put("education", eduScore);
        total += eduScore;

        // Income
        int incomeScore = scoreIncome(myPref, candidate.getAnnualIncomeInr());
        breakdown.put("income", incomeScore);
        total += incomeScore;

        // Location
        int locationScore = scoreLocation(myPref, candBase);
        breakdown.put("location", locationScore);
        total += locationScore;

        // Manglik
        int manglikScore = scoreManglik(myPref, candidate.getManglikStatus());
        breakdown.put("manglik", manglikScore);
        total += manglikScore;

        // Mutual preference boost (check if candidate's pref also matches me)
        BaseUserProfile myBase = baseProfileRepo.findByCognitoSub(userId).orElse(null);
        if (candPref != null && myBase != null && isMutualPreferenceMatch(candPref, me, myBase)) {
            breakdown.put("mutualBoost", SCORE_MUTUAL_BOOST);
            total += SCORE_MUTUAL_BOOST;
        }

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
        vo.setMatchCategory(MatchCategory.PROFESSIONAL_MATRIMONY);
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
        MatrimonialExtProfile p = matrimonialRepo.findByCognitoSubB(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getDietaryHabits()))    parts.add(p.getDietaryHabits());
        if (StringUtils.isNotEmpty(p.getHighestEducation())) parts.add(p.getHighestEducation());
        if (p.getAnnualIncomeInr() != null) {
            long lpa = p.getAnnualIncomeInr().longValue() / 100000;
            parts.add(lpa + " LPA");
        }
        if (StringUtils.isNotEmpty(p.getNativeCity()))       parts.add(p.getNativeCity());
        return String.join(" · ", parts);
    }

    // ── Scoring helpers ────────────────────────────────────────────────────────

    private int scoreReligion(PartnerPreference pref, String candidateReligion) {
        if (pref == null || StringUtils.isEmpty(pref.getReligionPref())) return SCORE_RELIGION;
        return pref.getReligionPref().contains(candidateReligion) ? SCORE_RELIGION : 0;
    }

    private int scoreCaste(PartnerPreference pref, String candCaste, String candGotram, String myGotram) {
        if (pref == null || StringUtils.isEmpty(pref.getCastePref())) return 0;
        int score = 0;
        if (pref.getCastePref().contains(candCaste)) score += 10;
        // Gotram must NOT match (same gotram = prohibited in many South Indian traditions)
        if (StringUtils.isNotEmpty(myGotram) && StringUtils.isNotEmpty(candGotram)
                && !myGotram.equalsIgnoreCase(candGotram)) {
            score += 5;
        }
        return Math.min(score, SCORE_CASTE);
    }

    private int scoreMotherTongue(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        return mine.equalsIgnoreCase(theirs) ? SCORE_MOTHER_TONGUE : 0;
    }

    private int scoreDiet(PartnerPreference pref, String candDiet) {
        if (pref == null || StringUtils.isEmpty(pref.getDietaryPref())) return SCORE_DIET;
        return pref.getDietaryPref().contains(candDiet) ? SCORE_DIET : 0;
    }

    private int scoreEducation(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        // Tier map: PhD=4, MBA/MD=3, BTech/BE/BSc=2, Diploma=1, Other=0
        Map<String, Integer> tierMap = Map.of(
            "PhD", 4, "MD", 3, "MBBS", 3, "MBA", 3, "M.Tech", 3,
            "B.Tech", 2, "BE", 2, "BSc", 2, "BA", 2, "Diploma", 1
        );
        int myTier   = tierMap.getOrDefault(mine,   0);
        int theirTier = tierMap.getOrDefault(theirs, 0);
        int diff = Math.abs(myTier - theirTier);
        if (diff == 0) return SCORE_EDUCATION;
        if (diff == 1) return SCORE_EDUCATION / 2;
        return 0;
    }

    private int scoreIncome(PartnerPreference pref, java.math.BigDecimal candIncome) {
        if (pref == null || candIncome == null) return 0;
        if (pref.getIncomeMinInr() == null && pref.getIncomeMaxInr() == null) return SCORE_INCOME;
        boolean aboveMin = pref.getIncomeMinInr() == null || candIncome.compareTo(pref.getIncomeMinInr()) >= 0;
        boolean belowMax = pref.getIncomeMaxInr() == null || candIncome.compareTo(pref.getIncomeMaxInr()) <= 0;
        return (aboveMin && belowMax) ? SCORE_INCOME : 0;
    }

    private int scoreLocation(PartnerPreference pref, BaseUserProfile candBase) {
        if (pref == null) return 0;
        if (StringUtils.isNotEmpty(pref.getPreferredStates())
                && pref.getPreferredStates().contains(candBase.getCurrentState())) {
            return SCORE_LOCATION;
        }
        if (StringUtils.isNotEmpty(pref.getPreferredCountries())
                && pref.getPreferredCountries().contains(candBase.getCurrentCountry())) {
            return SCORE_LOCATION / 2;
        }
        return 0;
    }

    private int scoreManglik(PartnerPreference pref, String candManglik) {
        if (pref == null || StringUtils.isEmpty(pref.getManglikPref())) return 0;
        if ("Any".equalsIgnoreCase(pref.getManglikPref())) return SCORE_MANGLIK;
        return pref.getManglikPref().equalsIgnoreCase(candManglik) ? SCORE_MANGLIK : 0;
    }

    private boolean isMutualPreferenceMatch(PartnerPreference candPref,
                                             MatrimonialExtProfile me,
                                             BaseUserProfile myBase) {
        // Check if candidate's preferences also accept my profile
        int myAge = Period.between(myBase.getDateOfBirth(), LocalDate.now()).getYears();
        if (candPref.getAgeMin() != null && myAge < candPref.getAgeMin()) return false;
        if (candPref.getAgeMax() != null && myAge > candPref.getAgeMax()) return false;
        if (StringUtils.isNotEmpty(candPref.getReligionPref())
                && !candPref.getReligionPref().contains(me.getReligion())) return false;
        return true;
    }
}
