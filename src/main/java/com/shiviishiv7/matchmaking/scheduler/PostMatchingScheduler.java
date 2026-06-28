package com.shiviishiv7.matchmaking.scheduler;

import com.shiviishiv7.matchmaking.common.enums.IntentType;
import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.enums.PostStatus;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.implementation.PartnerPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPostRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.PartnerPreference;
import com.shiviishiv7.matchmaking.provider.model.UserPost;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.service.email.EmailService;
import com.shiviishiv7.matchmaking.service.zoom.ZoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Two-phase matching system:
 *
 * Phase 1 — findAndScoreMatches() every 15 min:
 *   Finds all compatible candidates for each active post, scores them (0–100),
 *   saves as PENDING MATCH_RESULT rows. Skips pairs that already exist.
 *
 * Phase 2 — sendDailyTopMatch() every day at 9 AM IST:
 *   For each active post with PENDING matches, picks the top scorer,
 *   creates a Zoom meeting, and emails both users.
 *   Skips users who already have a meeting today.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PostMatchingScheduler {

    private static final int MEETING_DURATION_MINUTES = 30;
    private static final DateTimeFormatter EMAIL_DT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");

    private final UserPostRepository       userPostRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final MatchResultRepository    matchResultRepository;
    private final MeetingRepository        meetingRepository;
    private final BaseUserProfileRepository baseUserProfileRepository;
    private final ZoomService              zoomService;
    private final EmailService             emailService;

    // ─── Phase 1: expire stale posts ─────────────────────────────────────────

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void expireStalePosts() {
        List<UserPost> expired = userPostRepository.findExpiredActivePosts(LocalDateTime.now());
        for (UserPost post : expired) {
            post.setStatus(PostStatus.EXPIRED);
            userPostRepository.save(post);
            log.info("Post {} expired (30-day limit)", post.getId());
        }
    }

    // ─── Phase 1: find + score + save PENDING matches ────────────────────────

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void findAndScoreMatches() {
        List<UserPost> activePosts = userPostRepository.findActivePostsForMatching(LocalDateTime.now());
        log.info("PostMatchingScheduler [find]: processing {} active posts", activePosts.size());

        for (UserPost post : activePosts) {
            try {
                scoreAndSaveCandidates(post);
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: scoring failed for post {}: {}", post.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void scoreAndSaveCandidates(UserPost post) {
        Optional<PartnerPreference> prefOpt = partnerPreferenceRepository.findByPostId(post.getId());
        PartnerPreference pref = prefOpt.orElse(null);

        Optional<BaseUserProfile> requesterProfileOpt =
                baseUserProfileRepository.findByCognitoSub(post.getCognitoSub());
        if (requesterProfileOpt.isEmpty()) {
            log.warn("No BASE_USER_PROFILE for post owner {} — skipping", post.getCognitoSub());
            return;
        }
        BaseUserProfile requesterProfile = requesterProfileOpt.get();

        // All other active posts with same intent
        List<UserPost> candidatePosts = userPostRepository
                .findActivePostsForMatching(LocalDateTime.now())
                .stream()
                .filter(p -> !p.getCognitoSub().equals(post.getCognitoSub()))
                .filter(p -> p.getIntent() == post.getIntent())
                .collect(Collectors.toList());

        // Deduplicate by user (one candidate per user, best post first)
        Map<String, UserPost> byUser = new LinkedHashMap<>();
        for (UserPost cp : candidatePosts) {
            byUser.putIfAbsent(cp.getCognitoSub(), cp);
        }

        int saved = 0;
        for (Map.Entry<String, UserPost> entry : byUser.entrySet()) {
            String candidateSub = entry.getKey();
            UserPost candidatePost = entry.getValue();

            // Dedup: skip if this user pair already exists in MATCH_RESULT (either direction)
            if (matchResultRepository.existsByUserPair(post.getCognitoSub(), candidateSub)) {
                log.debug("Pair ({}, {}) already matched — skipping", post.getCognitoSub(), candidateSub);
                continue;
            }

            Optional<BaseUserProfile> candidateProfileOpt =
                    baseUserProfileRepository.findByCognitoSub(candidateSub);
            if (candidateProfileOpt.isEmpty()) continue;
            BaseUserProfile candidateProfile = candidateProfileOpt.get();

            // Hard filter: gender preference
            if (!passesGenderFilter(pref, candidateProfile)) continue;

            // Score
            Optional<PartnerPreference> candidatePrefOpt =
                    partnerPreferenceRepository.findByPostId(candidatePost.getId());
            int score = computeScore(post.getIntent(), pref, requesterProfile,
                    candidatePrefOpt.orElse(null), candidateProfile);

            // Save as PENDING with score
            MatchResult mr = MatchResult.builder()
                    .postId(post.getId())
                    .cognitoSubA(post.getCognitoSub())
                    .cognitoSubB(candidateSub)
                    .matchCategory(post.getIntent() == IntentType.DATING
                            ? MatchCategory.CASUAL_DATING
                            : MatchCategory.PROFESSIONAL_MATRIMONY)
                    .compatibilityScore((double) score)
                    .scoreBreakdown("score=" + score)
                    .status(MatchStatus.PENDING)
                    .roundCount(0)
                    .maxRounds(3)
                    .build();
            matchResultRepository.save(mr);
            saved++;
            log.info("Saved PENDING match: post={} userA={} userB={} score={}",
                    post.getId(), post.getCognitoSub(), candidateSub, score);
        }

        if (saved > 0) log.info("Post {}: saved {} new PENDING matches", post.getId(), saved);
    }

    // ─── Phase 2: daily job — send top 1 match per post ──────────────────────

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sendDailyTopMatch() {
        log.info("PostMatchingScheduler [daily]: running top-match email job");
        List<UserPost> activePosts = userPostRepository.findActivePostsForMatching(LocalDateTime.now());

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay   = today.plusDays(1).atStartOfDay();

        for (UserPost post : activePosts) {
            try {
                processDailyTopMatch(post, startOfDay, endOfDay);
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: daily job failed for post {}: {}", post.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void processDailyTopMatch(UserPost post, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        // Skip if this user already has a meeting today
        if (meetingRepository.hasScheduledMeetingToday(post.getCognitoSub(), startOfDay, endOfDay)) {
            log.info("Post {}: user {} already has a meeting today — skipping", post.getId(), post.getCognitoSub());
            return;
        }

        // Get top PENDING match by score
        List<MatchResult> pending = matchResultRepository.findPendingByPostIdOrderByScoreDesc(post.getId());
        if (pending.isEmpty()) {
            log.info("Post {}: no PENDING matches yet", post.getId());
            return;
        }

        MatchResult topMatch = pending.get(0);
        String candidateSub = topMatch.getCognitoSubB();

        // Skip if candidate already has a meeting today
        if (meetingRepository.hasScheduledMeetingToday(candidateSub, startOfDay, endOfDay)) {
            log.info("Post {}: top candidate {} already has a meeting today — trying next", post.getId(), candidateSub);
            // Try next best candidate
            for (int i = 1; i < pending.size(); i++) {
                MatchResult next = pending.get(i);
                if (!meetingRepository.hasScheduledMeetingToday(next.getCognitoSubB(), startOfDay, endOfDay)) {
                    topMatch = next;
                    candidateSub = next.getCognitoSubB();
                    break;
                }
            }
            if (topMatch.getCognitoSubB().equals(pending.get(0).getCognitoSubB())) {
                log.info("Post {}: all top candidates busy today — skipping", post.getId());
                return;
            }
        }

        // Schedule Zoom + email
        LocalDateTime meetingTime = nextValidSlot(LocalDateTime.now());
        scheduleZoomAndEmail(topMatch, post, meetingTime);

        // Update post matchCount
        post.setMatchCount(post.getMatchCount() + 1);
        if (post.getMatchCount() >= 5) post.setStatus(PostStatus.CLOSED);
        userPostRepository.save(post);

        // Also increment the candidate's post matchCount
        userPostRepository.findByCognitoSubAndStatusOrderByCreatedAtDesc(candidateSub, PostStatus.ACTIVE)
                .stream().findFirst().ifPresent(candidatePost -> {
                    candidatePost.setMatchCount(candidatePost.getMatchCount() + 1);
                    if (candidatePost.getMatchCount() >= 5) candidatePost.setStatus(PostStatus.CLOSED);
                    userPostRepository.save(candidatePost);
                });
    }

    private void scheduleZoomAndEmail(MatchResult match, UserPost post, LocalDateTime meetingTime) {
        String topic = "Shall We Connect — " +
                (post.getIntent() == IntentType.DATING ? "Dating" : "Matrimonial") + " Meeting";

        ZoomService.ZoomMeetingResult zoom = zoomService.createMeeting(topic, meetingTime, MEETING_DURATION_MINUTES);

        Meeting meeting = Meeting.builder()
                .matchResultId(match.getId())
                .roundNumber(match.getRoundCount() + 1)
                .scheduledAt(meetingTime)
                .durationMinutes(MEETING_DURATION_MINUTES)
                .meetingType(MeetingType.SCHEDULED)
                .status(MeetingStatus.SCHEDULED)
                .zoomMeetingId(zoom.getMeetingId())
                .zoomJoinUrl(zoom.getJoinUrl())
                .zoomStartUrl(zoom.getStartUrl())
                .zoomPassword(zoom.getPassword())
                .build();
        meetingRepository.save(meeting);

        match.setStatus(MatchStatus.MEETING_SCHEDULED);
        match.setRoundCount(match.getRoundCount() + 1);
        matchResultRepository.save(match);

        String formattedTime = meetingTime.format(EMAIL_DT);
        Optional<BaseUserProfile> profileA = baseUserProfileRepository.findByCognitoSub(match.getCognitoSubA());
        Optional<BaseUserProfile> profileB = baseUserProfileRepository.findByCognitoSub(match.getCognitoSubB());

        profileA.ifPresent(a -> profileB.ifPresent(b -> {
            emailService.sendMeetingScheduledEmail(a.getEmail(), a.getName(), b.getName(), zoom.getJoinUrl(), formattedTime);
            emailService.sendMeetingScheduledEmail(b.getEmail(), b.getName(), a.getName(), zoom.getJoinUrl(), formattedTime);
        }));

        log.info("Scheduled Zoom meeting for match {} (score={}) at {} — Zoom ID: {}",
                match.getId(), match.getCompatibilityScore(), meetingTime, zoom.getMeetingId());
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    private int computeScore(IntentType intent, PartnerPreference myPref,
                              BaseUserProfile myProfile,
                              PartnerPreference theirPref,
                              BaseUserProfile theirProfile) {
        return intent == IntentType.DATING
                ? scoreDating(myPref, myProfile, theirPref, theirProfile)
                : scoreMatrimonial(myPref, myProfile, theirPref, theirProfile);
    }

    private int scoreDating(PartnerPreference myPref, BaseUserProfile myProfile,
                             PartnerPreference theirPref, BaseUserProfile theirProfile) {
        int score = 0;

        // City match (30 pts)
        if (myProfile.getCurrentCity() != null && theirProfile.getCurrentCity() != null
                && myProfile.getCurrentCity().equalsIgnoreCase(theirProfile.getCurrentCity())) {
            score += 30;
        } else if (myProfile.getCurrentState() != null && theirProfile.getCurrentState() != null
                && myProfile.getCurrentState().equalsIgnoreCase(theirProfile.getCurrentState())) {
            score += 10; // same state partial credit
        }

        // Age in preferred range (25 pts)
        if (myPref != null && theirProfile.getDateOfBirth() != null) {
            int theirAge = LocalDate.now().getYear() - theirProfile.getDateOfBirth().getYear();
            boolean ageOk = (myPref.getAgeMin() == null || theirAge >= myPref.getAgeMin())
                    && (myPref.getAgeMax() == null || theirAge <= myPref.getAgeMax());
            if (ageOk) score += 25;
        }

        // Relationship goal match (20 pts) — mutual
        if (myPref != null && theirPref != null
                && myPref.getRelationshipGoalPref() != null && theirPref.getRelationshipGoalPref() != null
                && myPref.getRelationshipGoalPref().equalsIgnoreCase(theirPref.getRelationshipGoalPref())) {
            score += 20;
        }

        // Diet compatible (15 pts)
        score += scoreDiet(myPref, theirPref);

        // Smoking compatible (5 pts)
        score += smokingScore(myPref, theirPref);

        // Drinking compatible (5 pts)
        score += drinkingScore(myPref, theirPref);

        return Math.min(score, 100);
    }

    private int scoreMatrimonial(PartnerPreference myPref, BaseUserProfile myProfile,
                                  PartnerPreference theirPref, BaseUserProfile theirProfile) {
        int score = 0;

        // City / state (25 pts)
        if (myProfile.getCurrentCity() != null && theirProfile.getCurrentCity() != null
                && myProfile.getCurrentCity().equalsIgnoreCase(theirProfile.getCurrentCity())) {
            score += 25;
        } else if (myProfile.getCurrentState() != null && theirProfile.getCurrentState() != null
                && myProfile.getCurrentState().equalsIgnoreCase(theirProfile.getCurrentState())) {
            score += 15;
        }

        // Age in preferred range (20 pts)
        if (myPref != null && theirProfile.getDateOfBirth() != null) {
            int theirAge = LocalDate.now().getYear() - theirProfile.getDateOfBirth().getYear();
            boolean ageOk = (myPref.getAgeMin() == null || theirAge >= myPref.getAgeMin())
                    && (myPref.getAgeMax() == null || theirAge <= myPref.getAgeMax());
            if (ageOk) score += 20;
        }

        // Religion match (20 pts)
        if (myPref != null && myPref.getReligionPref() != null && theirPref != null
                && myPref.getReligionPref().equalsIgnoreCase(theirPref.getReligionPref())) {
            score += 20;
        }

        // Diet compatible (15 pts)
        score += scoreDiet(myPref, theirPref);

        // Family values (10 pts)
        if (myPref != null && theirPref != null
                && myPref.getFamilyValuesPref() != null && theirPref.getFamilyValuesPref() != null
                && myPref.getFamilyValuesPref().equalsIgnoreCase(theirPref.getFamilyValuesPref())) {
            score += 10;
        }

        // Marital status (5 pts)
        if (myPref != null && myPref.getMaritalStatusPref() != null
                && theirPref != null && theirPref.getMaritalStatusPref() != null
                && myPref.getMaritalStatusPref().equalsIgnoreCase(theirPref.getMaritalStatusPref())) {
            score += 5;
        }

        // Wants children mutual (5 pts)
        if (myPref != null && theirPref != null
                && myPref.getWantsChildrenPref() != null && theirPref.getWantsChildrenPref() != null
                && myPref.getWantsChildrenPref().equals(theirPref.getWantsChildrenPref())) {
            score += 5;
        }

        return Math.min(score, 100);
    }

    // ─── Scoring helpers ──────────────────────────────────────────────────────

    private int scoreDiet(PartnerPreference myPref, PartnerPreference theirPref) {
        if (myPref == null || theirPref == null) return 0;
        if (myPref.getDietaryPref() == null || theirPref.getDietaryPref() == null) return 15; // no pref = ok
        return myPref.getDietaryPref().equalsIgnoreCase(theirPref.getDietaryPref()) ? 15 : 0;
    }

    private int smokingScore(PartnerPreference myPref, PartnerPreference theirPref) {
        if (myPref == null || theirPref == null) return 0;
        if (myPref.getSmokingPref() == null || theirPref.getSmokingPref() == null) return 5;
        return myPref.getSmokingPref().equalsIgnoreCase(theirPref.getSmokingPref()) ? 5 : 0;
    }

    private int drinkingScore(PartnerPreference myPref, PartnerPreference theirPref) {
        if (myPref == null || theirPref == null) return 0;
        if (myPref.getDrinkingPref() == null || theirPref.getDrinkingPref() == null) return 5;
        return myPref.getDrinkingPref().equalsIgnoreCase(theirPref.getDrinkingPref()) ? 5 : 0;
    }

    private boolean passesGenderFilter(PartnerPreference pref, BaseUserProfile candidate) {
        if (pref == null || pref.getGenderPref() == null
                || pref.getGenderPref().isBlank()
                || pref.getGenderPref().equalsIgnoreCase("Any")) return true;
        if (candidate.getGender() == null) return true;
        return candidate.getGender().name().equalsIgnoreCase(pref.getGenderPref());
    }

    // ─── Meeting time window ──────────────────────────────────────────────────

    /**
     * Valid window: 11:00 AM – 2:00 AM (crosses midnight).
     * Blackout:     2:00 AM – 10:59 AM.
     */
    static LocalDateTime nextValidSlot(LocalDateTime from) {
        LocalDateTime candidate = from.plusHours(3);
        int hour = candidate.getHour();
        if (hour >= 2 && hour < 11) {
            candidate = candidate.toLocalDate().atTime(11, 0);
        }
        return candidate;
    }
}
