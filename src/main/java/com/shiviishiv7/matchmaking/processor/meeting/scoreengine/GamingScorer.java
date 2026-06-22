package com.shiviishiv7.matchmaking.processor.meeting.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.GamingExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.GamingExtProfile;
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
 * Gaming scoring weights (total = 100 pts):
 *
 *   Platform match              30 pts  — must share at least one platform (hard filter)
 *   Game overlap                25 pts  — same games = instant session partners
 *   Genre overlap               20 pts  — same genres = compatible taste even for new games
 *   Schedule compatibility      15 pts  — same gaming windows = can actually play together
 *   Skill level match           10 pts  — newbie + pro = frustrating; same = smooth
 */
@Component
@Slf4j
public class GamingScorer implements CategoryScorer {

    private static final int SCORE_PLATFORM  = 30;
    private static final int SCORE_GAMES     = 25;
    private static final int SCORE_GENRE     = 20;
    private static final int SCORE_SCHEDULE  = 15;
    private static final int SCORE_SKILL     = 10;

    @Autowired
    private GamingExtProfileRepository gamingRepo;

    @Autowired
    private BaseUserProfileRepository baseProfileRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() {
        return MatchCategory.GAMING_BUDDIES;
    }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        log.trace("Fetching gaming candidates for userId: {}", userId);

        GamingExtProfile me = gamingRepo.findByCognitoSub(userId).orElse(null);
        if (me == null) {
            log.warn("Gaming profile missing for userId: {}. Returning empty.", userId);
            return Collections.emptyList();
        }

        List<String> result = gamingRepo.findAll().stream()
                .filter(c -> !c.getCognitoSub().equals(userId))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSub()))
                // Hard filter: must share at least one platform — cross-platform gaming is rare
                .filter(c -> hasOverlap(me.getPlatforms(), c.getPlatforms()))
                .map(GamingExtProfile::getCognitoSub)
                .collect(Collectors.toList());

        log.trace("Gaming hard filter: {} candidates remain for userId: {}", result.size(), userId);
        return result;
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        GamingExtProfile me        = gamingRepo.findByCognitoSub(userId).orElseThrow();
        GamingExtProfile candidate = gamingRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile candBase   = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        int platformScore = scoreOverlap(me.getPlatforms(), candidate.getPlatforms(), SCORE_PLATFORM, 3);
        breakdown.put("platform", platformScore);
        total += platformScore;

        int gameScore = scoreOverlap(me.getFavoriteGames(), candidate.getFavoriteGames(), SCORE_GAMES, 2);
        breakdown.put("games", gameScore);
        total += gameScore;

        int genreScore = scoreOverlap(me.getFavoriteGenres(), candidate.getFavoriteGenres(), SCORE_GENRE, 2);
        breakdown.put("genres", genreScore);
        total += genreScore;

        int scheduleScore = scoreSchedule(me.getGamingSchedule(), candidate.getGamingSchedule());
        breakdown.put("schedule", scheduleScore);
        total += scheduleScore;

        int skillScore = scoreSkill(me.getSkillLevel(), candidate.getSkillLevel(),
                me.getIsOkWithNewbies(), candidate.getIsOkWithNewbies());
        breakdown.put("skillLevel", skillScore);
        total += skillScore;

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
        vo.setMatchCategory(MatchCategory.GAMING_BUDDIES);
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
        GamingExtProfile p = gamingRepo.findByCognitoSub(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getPlatforms()))       parts.add(p.getPlatforms());
        if (StringUtils.isNotEmpty(p.getFavoriteGenres()))  parts.add(p.getFavoriteGenres());
        if (StringUtils.isNotEmpty(p.getSkillLevel()))      parts.add(p.getSkillLevel());
        if (StringUtils.isNotEmpty(p.getGamingSchedule())) parts.add(p.getGamingSchedule());
        return String.join(" · ", parts);
    }

    // ── Scoring helpers ────────────────────────────────────────────────────────

    private boolean hasOverlap(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return false;
        Set<String> a = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> b = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        a.retainAll(b);
        return !a.isEmpty();
    }

    /**
     * Generic overlap scorer.
     * ptsPerMatch = points awarded per overlapping item, capped at maxScore.
     */
    private int scoreOverlap(String mine, String theirs, int maxScore, int ptsPerMatch) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Set<String> mySet    = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        mySet.retainAll(theirSet);
        return Math.min(mySet.size() * ptsPerMatch, maxScore);
    }

    private int scoreSchedule(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        // "Weekday Nights", "Weekends", "Anytime"
        if (mine.equalsIgnoreCase(theirs)) return SCORE_SCHEDULE;
        if ("Anytime".equalsIgnoreCase(mine) || "Anytime".equalsIgnoreCase(theirs))
            return SCORE_SCHEDULE / 2;
        return 0;
    }

    private int scoreSkill(String mine, String theirs, Boolean myOkWithNewbies, Boolean theirOkWithNewbies) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Map<String, Integer> skillMap = Map.of(
                "CASUAL", 1, "SEMI-PRO", 2, "COMPETITIVE", 3);
        int myTier    = skillMap.getOrDefault(mine.toUpperCase(), 1);
        int theirTier = skillMap.getOrDefault(theirs.toUpperCase(), 1);
        int diff = Math.abs(myTier - theirTier);
        if (diff == 0) return SCORE_SKILL;
        // If the more experienced player is ok with newbies, partial score
        if (diff == 1) {
            boolean higherIsOk = (myTier > theirTier)
                    ? Boolean.TRUE.equals(myOkWithNewbies)
                    : Boolean.TRUE.equals(theirOkWithNewbies);
            return higherIsOk ? SCORE_SKILL / 2 : 0;
        }
        return 0;
    }
}
