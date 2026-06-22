package com.shiviishiv7.matchmaking.processor.meeting.scoreengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.processor.matchingengine.CategoryScorer;

import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.ProfessionalExtProfileRepository;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.ProfessionalExtProfile;
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
 * Professional scoring weights (total = 100 pts):
 *   Tech stack overlap          30 pts  — shared languages/frameworks = collab ready
 *   Skills seeking/offering     25 pts  — my offering matches their seeking & vice versa
 *   Industry domain             20 pts  — same domain = deeper context
 *   Mentorship role             15 pts  — mentor/mentee alignment
 *   Collab mode                 10 pts  — online vs in-person preference
 */
@Component
@Slf4j
public class ProfessionalScorer implements CategoryScorer {

    @Autowired private ProfessionalExtProfileRepository professionalRepo;
    @Autowired private BaseUserProfileRepository baseProfileRepo;
    @Autowired private ObjectMapper objectMapper;

    @Override
    public MatchCategory supports() { return MatchCategory.MENTORSHIP; }

    @Override
    public List<String> fetchCandidateIds(String userId, List<String> excludeIds) {
        return professionalRepo.findAll().stream()
            .filter(c -> !c.getCognitoSub().equals(userId))
            .filter(c -> excludeIds == null || !excludeIds.contains(c.getCognitoSub()))
            .map(ProfessionalExtProfile::getCognitoSub)
            .collect(Collectors.toList());
    }

    @Override
    public MatchCandidateVO score(String userId, String candidateUserId) {
        ProfessionalExtProfile me        = professionalRepo.findByCognitoSub(userId).orElseThrow();
        ProfessionalExtProfile candidate = professionalRepo.findByCognitoSub(candidateUserId).orElseThrow();
        BaseUserProfile candBase         = baseProfileRepo.findByCognitoSub(candidateUserId).orElseThrow();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int total = 0;

        int techScore = scoreOverlap(me.getTechStack(), candidate.getTechStack(), 30);
        breakdown.put("techStack", techScore); total += techScore;

        int skillScore = scoreSkillExchange(me.getSkillsOffering(), me.getSkillsSeeking(),
                                            candidate.getSkillsOffering(), candidate.getSkillsSeeking());
        breakdown.put("skillExchange", skillScore); total += skillScore;

        int domainScore = (StringUtils.isNotEmpty(me.getIndustryDomain())
                && me.getIndustryDomain().equalsIgnoreCase(candidate.getIndustryDomain())) ? 20 : 0;
        breakdown.put("domain", domainScore); total += domainScore;

        int mentorScore = scoreMentorshipRole(me.getMentorshipRole(), candidate.getMentorshipRole());
        breakdown.put("mentorshipRole", mentorScore); total += mentorScore;

        int collabScore = (StringUtils.isNotEmpty(me.getPreferredCollabMode())
                && me.getPreferredCollabMode().equalsIgnoreCase(candidate.getPreferredCollabMode())) ? 10 : 5;
        breakdown.put("collabMode", collabScore); total += collabScore;

        total = Math.min(total, 100);

        MatchCandidateVO vo = new MatchCandidateVO();
        vo.setCognitoSubB(candidateUserId);
        vo.setName(candBase.getName());
        vo.setProfilePhotoUrl(candBase.getProfilePhotoUrl());
        vo.setTagline(candBase.getTagline());
        vo.setCurrentCity(candBase.getCurrentCity());
        vo.setCurrentCountry(candBase.getCurrentCountry());
        if (candBase.getDateOfBirth() != null)
            vo.setAge(Period.between(candBase.getDateOfBirth(), LocalDate.now()).getYears());
        vo.setCompatibilityScore(total);
        vo.setMatchCategory(MatchCategory.MENTORSHIP);
        try { vo.setScoreBreakdown(objectMapper.writeValueAsString(breakdown)); } catch (Exception ignored) {}
        vo.setCategorySnippet(buildSnippet(candidateUserId));
        return vo;
    }

    @Override
    public String buildSnippet(String candidateUserId) {
        ProfessionalExtProfile p = professionalRepo.findByCognitoSub(candidateUserId).orElse(null);
        if (p == null) return "";
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotEmpty(p.getCurrentRole())) parts.add(p.getCurrentRole());
        if (StringUtils.isNotEmpty(p.getIndustryDomain())) parts.add(p.getIndustryDomain());
        if (p.getYearsOfExperience() != null) parts.add(p.getYearsOfExperience() + " yrs exp");
        if (StringUtils.isNotEmpty(p.getMentorshipRole())) parts.add(p.getMentorshipRole());
        return String.join(" · ", parts);
    }

    private int scoreOverlap(String mine, String theirs, int maxScore) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        Set<String> mySet = new HashSet<>(Arrays.asList(mine.toLowerCase().split(",")));
        Set<String> theirSet = new HashSet<>(Arrays.asList(theirs.toLowerCase().split(",")));
        mySet.retainAll(theirSet);
        if (mySet.isEmpty()) return 0;
        return Math.min(mySet.size() * (maxScore / 3), maxScore);
    }

    private int scoreSkillExchange(String myOffering, String mySeeking,
                                    String theirOffering, String theirSeeking) {
        int score = 0;
        // Do I offer what they seek?
        if (StringUtils.isNotEmpty(myOffering) && StringUtils.isNotEmpty(theirSeeking)) {
            Set<String> offer = new HashSet<>(Arrays.asList(myOffering.toLowerCase().split(",")));
            Set<String> seek  = new HashSet<>(Arrays.asList(theirSeeking.toLowerCase().split(",")));
            offer.retainAll(seek);
            if (!offer.isEmpty()) score += 12;
        }
        // Do they offer what I seek?
        if (StringUtils.isNotEmpty(theirOffering) && StringUtils.isNotEmpty(mySeeking)) {
            Set<String> offer = new HashSet<>(Arrays.asList(theirOffering.toLowerCase().split(",")));
            Set<String> seek  = new HashSet<>(Arrays.asList(mySeeking.toLowerCase().split(",")));
            offer.retainAll(seek);
            if (!offer.isEmpty()) score += 13;
        }
        return Math.min(score, 25);
    }

    private int scoreMentorshipRole(String mine, String theirs) {
        if (StringUtils.isEmpty(mine) || StringUtils.isEmpty(theirs)) return 0;
        // Mentor + Mentee = perfect match (15pts); Both = good (10pts); Same role = ok (5pts)
        boolean complementary = (mine.equalsIgnoreCase("MENTOR") && theirs.equalsIgnoreCase("MENTEE"))
                             || (mine.equalsIgnoreCase("MENTEE") && theirs.equalsIgnoreCase("MENTOR"));
        if (complementary) return 15;
        if (mine.equalsIgnoreCase("BOTH") || theirs.equalsIgnoreCase("BOTH")) return 10;
        return mine.equalsIgnoreCase(theirs) ? 5 : 0;
    }
}
