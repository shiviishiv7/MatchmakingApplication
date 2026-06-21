package com.shiviishiv7.matchmaking.provider.model;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchengine.CategoryScorer;

import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.TravelExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.vo.MatchCandidateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Travel scoring weights (total = 100 pts):
 *   Travel style match          25 pts  — backpacker vs luxury mismatch is jarring
 *   Destination overlap         20 pts  — shared bucket list / upcoming trips
 *   Trip frequency compatibility 15 pts — 1/yr vs 10/yr is a lifestyle gap
 *   Budget accommodation match  15 pts  — ok with budget stays / camping
 *   Group size preference       15 pts  — solo vs group preference
 *   Diet compatibility          10 pts  — important when travelling together
 */
@Component
@Slf4j
public class TravelScorer implements CategoryScorer {

    @Autowired private TravelExtProfileRepository travelRepo;
    @Autowired private BaseUserProfileRepository baseProfileRepo;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() { return MatchCategory.TRAVEL_TREKKING; }

    @Override
    public List<Integer> fetchCandidateIds(Integer userId, List<Integer> excludeIds) {
        return travelRepo.findAll().stream()
            .filter(c -> !c.getUserId().equals(userId))
            .filter(c -> excludeIds == null || !excludeIds.contains(c.getUserId()))
            .map(TravelExtProfile::getUserId)
            .collect(Collectors.toList());
    }

    @Override
    public MatchCandidateVO score(Integer userId, Integer candidateUserId) {
        TravelExtProfile me        = travelRepo.findByUserId(userId).orElseThrow();
        TravelExtProfile candidate = travelRepo.findByUserId(candidateUserId).orElseThrow();
        BaseUserProfile candBase   = baseProfileRepo.findByUserId(candidateUserId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        int styleScore = scoreStyle(me.getTravelStyle(), candidate.getTravelStyle());
        breakdown.put("travelStyle", styleScore); total += styleScore;

        int destScore = scoreDestinations(me.getPreferredDestinations(), candidate.getPreferredDestinations());
        breakdown.put("destinations", destScore); total += destScore;

        int freqScore = scoreFrequency(me.getTripsPerYear(), candidate.getTripsPerYear());
        breakdown.put("tripFrequency", freqScore); total += freqScore;

        int budgetScore = scoreBudget(me.getIsOkWithBudgetStays(), candidate.getIsOkWithBudgetStays(),
                                      me.getIsOkWithCamping(), candidate.getIsOkWithCamping());
        breakdown.put("budget", budgetScore); total += budgetScore;

        int groupScore = scoreGroupSize(me.getPreferredGroupSize(), candidate.getPreferredGroupSize());
        breakdown.put("groupSize", groupScore); total += groupScore;

        int dietScore = (StringUtils.isNotEmpty(me.getDietaryNeeds())
                && me.getDietaryNeeds().equalsIgnoreCase(candidate.getDietaryNeeds())) ? 10 : 0;
        breakdown.put("diet", dietScore); total += dietScore;

        total = Math.min(total, 100);

        MatchCandidateVO vo = buildBaseVO(candidateUserId, candBase, total, MatchCategory.TRAVEL_TREKKING);
        try { vo.setScoreBreakdown(objectMapper.writeValueAsString(breakdown)); } catch (Exception ignored) {}
        vo.setCategorySnippet(buildSnippet(candidateUserId));
        return vo;
    }

    @Override
    public String buildSnippet(Integer candidateUserId) {
        TravelExtProfile p = travelRepo.findByUserId(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getTravelStyle())) parts.add(p.getTravelStyle());
        if (p.getTripsPerYear() != null) parts.add(p.getTripsPerYear() + " trips/yr");
        if (p.getCountriesVisited() != null) parts.add(p.getCountriesVisited() + " countries");
        return String.join(" · ", parts);
    }

    private int scoreStyle(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        return mine.equalsIgnoreCase(theirs) ? 25 : 0;
    }

    private int scoreDestinations(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Set<String> mySet = new HashSet<>(Arrays.asList(mine.split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.split(",")));
        mySet.retainAll(theirSet);
        return mySet.isEmpty() ? 0 : Math.min(mySet.size() * 7, 20);
    }

    private int scoreFrequency(Integer mine, Integer theirs) {
        if (mine == null || theirs == null) return 0;
        int diff = Math.abs(mine - theirs);
        if (diff == 0) return 15;
        if (diff <= 2) return 8;
        return 0;
    }

    private int scoreBudget(Boolean myBudget, Boolean theirBudget, Boolean myCamping, Boolean theirCamping) {
        int score = 0;
        if (myBudget != null && myBudget.equals(theirBudget)) score += 8;
        if (myCamping != null && myCamping.equals(theirCamping)) score += 7;
        return Math.min(score, 15);
    }

    private int scoreGroupSize(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        return mine.equalsIgnoreCase(theirs) ? 15 : 0;
    }

    private MatchCandidateVO buildBaseVO(Integer candidateUserId, BaseUserProfile base,
                                          int score, MatchCategory category) {
        MatchCandidateVO vo = new MatchCandidateVO();
        vo.setCandidateUserId(candidateUserId);
        vo.setDisplayName(base.getDisplayName());
        vo.setProfilePhotoUrl(base.getProfilePhotoUrl());
        vo.setTagline(base.getTagline());
        vo.setCurrentCity(base.getCurrentCity());
        vo.setCurrentCountry(base.getCurrentCountry());
        if (base.getDateOfBirth() != null)
            vo.setAge(Period.between(base.getDateOfBirth(), LocalDate.now()).getYears());
        vo.setGender(base.getGender() != null ? base.getGender().name() : null);
        vo.setCompatibilityScore(score);
        vo.setMatchCategory(category);
        return vo;
    }
}
