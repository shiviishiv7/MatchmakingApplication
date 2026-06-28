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
import com.shiviishiv7.matchmaking.provider.model.profile.DatingExtProfile;
import com.shiviishiv7.matchmaking.provider.model.profile.MatrimonialExtProfile;
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
 * Runs every 15 minutes.
 * Finds all ACTIVE posts with matchCount < 5 and not expired,
 * discovers compatible candidates, creates Zoom meetings (staggered by 1 day),
 * and notifies both parties by email.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PostMatchingScheduler {

    private static final int MAX_MATCHES_PER_POST = 5;
    private static final int MEETING_DURATION_MINUTES = 30;
    private static final DateTimeFormatter EMAIL_DT = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' hh:mm a");

    private final UserPostRepository userPostRepository;
    private final PartnerPreferenceRepository partnerPreferenceRepository;
    private final MatchResultRepository matchResultRepository;
    private final MeetingRepository meetingRepository;
    private final BaseUserProfileRepository baseUserProfileRepository;
    private final ZoomService zoomService;
    private final EmailService emailService;

    // Lazy-load ext profile repos via Spring
    @org.springframework.beans.factory.annotation.Autowired
    private com.shiviishiv7.matchmaking.provider.implementation.DatingExtProfileRepository datingExtProfileRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.shiviishiv7.matchmaking.provider.implementation.MatrimonialExtProfileRepository matrimonialExtProfileRepository;

    // ── expire stale posts ────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void expireStalePosts() {
        List<UserPost> expired = userPostRepository.findExpiredActivePosts(LocalDateTime.now());
        for (UserPost post : expired) {
            post.setStatus(PostStatus.EXPIRED);
            userPostRepository.save(post);
            log.info("Post {} expired (30-day limit reached)", post.getId());
        }
    }

    // ── main matching job ─────────────────────────────────────────────────────

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void matchActivePosts() {
        List<UserPost> activePosts = userPostRepository.findActivePostsForMatching(LocalDateTime.now());
        log.info("PostMatchingScheduler: processing {} active posts", activePosts.size());

        for (UserPost post : activePosts) {
            try {
                processPost(post);
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: Failed to process post {}: {}", post.getId(), ex.getMessage(), ex);
            }
        }
    }

    // ── per-post processing ───────────────────────────────────────────────────

    private void processPost(UserPost post) {
        Optional<PartnerPreference> prefOpt = partnerPreferenceRepository.findByPostId(post.getId());
        if (prefOpt.isEmpty()) {
            log.warn("No partner preferences for post {} — skipping", post.getId());
            return;
        }
        PartnerPreference pref = prefOpt.get();

        // Already-matched candidate IDs for this post
        Set<String> alreadyMatched = matchResultRepository
                .findByPostId(post.getId())
                .stream()
                .map(MatchResult::getCognitoSubB)
                .collect(Collectors.toSet());
        alreadyMatched.add(post.getCognitoSub()); // exclude self

        int slotsLeft = MAX_MATCHES_PER_POST - post.getMatchCount();
        if (slotsLeft <= 0) {
            post.setStatus(PostStatus.CLOSED);
            userPostRepository.save(post);
            return;
        }

        List<String> candidates = findCandidates(post, pref, alreadyMatched, slotsLeft);
        if (candidates.isEmpty()) {
            log.info("No new candidates for post {}", post.getId());
            return;
        }

        // Base time for staggered scheduling: first match uses next valid slot from now,
        // each subsequent match adds one day.
        LocalDateTime baseSlot = nextValidSlot(LocalDateTime.now());
        int dayOffset = 0;

        for (String candidateSub : candidates) {
            try {
                scheduleMatch(post, candidateSub, baseSlot.plusDays(dayOffset));
                dayOffset++;
                post.setMatchCount(post.getMatchCount() + 1);
            } catch (Exception ex) {
                log.error("ALERT_FOR_ERROR: Failed to schedule match for post {} candidate {}: {}",
                        post.getId(), candidateSub, ex.getMessage(), ex);
            }
        }

        if (post.getMatchCount() >= MAX_MATCHES_PER_POST) {
            post.setStatus(PostStatus.CLOSED);
        }
        userPostRepository.save(post);
    }

    private void scheduleMatch(UserPost post, String candidateSub, LocalDateTime scheduledAt) {
        // Persist match result
        MatchResult mr = MatchResult.builder()
                .postId(post.getId())
                .cognitoSubA(post.getCognitoSub())
                .cognitoSubB(candidateSub)
                .matchCategory(post.getIntent() == IntentType.DATING
                        ? MatchCategory.CASUAL_DATING
                        : MatchCategory.PROFESSIONAL_MATRIMONY)
                .compatibilityScore(0.0)
                .status(MatchStatus.MEETING_SCHEDULED)
                .roundCount(1)
                .maxRounds(3)
                .build();
        mr = matchResultRepository.save(mr);

        // Create Zoom meeting
        String topic = "Shall We Connect — " +
                (post.getIntent() == IntentType.DATING ? "Dating" : "Matrimonial") + " Meeting";
        ZoomService.ZoomMeetingResult zoom = zoomService.createMeeting(topic, scheduledAt, MEETING_DURATION_MINUTES);

        // Persist meeting
        Meeting meeting = Meeting.builder()
                .matchResultId(mr.getId())
                .roundNumber(1)
                .scheduledAt(scheduledAt)
                .durationMinutes(MEETING_DURATION_MINUTES)
                .meetingType(MeetingType.SCHEDULED)
                .status(MeetingStatus.SCHEDULED)
                .zoomMeetingId(zoom.getMeetingId())
                .zoomJoinUrl(zoom.getJoinUrl())
                .zoomStartUrl(zoom.getStartUrl())
                .zoomPassword(zoom.getPassword())
                .build();
        meetingRepository.save(meeting);

        // Send emails to both users
        String formattedTime = scheduledAt.format(EMAIL_DT);
        sendMatchEmails(post.getCognitoSub(), candidateSub, zoom.getJoinUrl(), formattedTime);

        log.info("Matched post {} with candidate {} → Zoom meeting {} at {}",
                post.getId(), candidateSub, zoom.getMeetingId(), scheduledAt);
    }

    private void sendMatchEmails(String subA, String subB, String joinUrl, String formattedTime) {
        Optional<BaseUserProfile> profileA = baseUserProfileRepository.findByCognitoSub(subA);
        Optional<BaseUserProfile> profileB = baseUserProfileRepository.findByCognitoSub(subB);

        profileA.ifPresent(a -> profileB.ifPresent(b -> {
            emailService.sendMeetingScheduledEmail(a.getEmail(), a.getName(), b.getName(), joinUrl, formattedTime);
            emailService.sendMeetingScheduledEmail(b.getEmail(), b.getName(), a.getName(), joinUrl, formattedTime);
        }));
    }

    // ── candidate discovery ───────────────────────────────────────────────────

    private List<String> findCandidates(UserPost post, PartnerPreference pref,
                                         Set<String> exclude, int limit) {
        if (post.getIntent() == IntentType.DATING) {
            return findDatingCandidates(post, pref, exclude, limit);
        } else {
            return findMatrimonialCandidates(post, pref, exclude, limit);
        }
    }

    private List<String> findDatingCandidates(UserPost post, PartnerPreference pref,
                                               Set<String> exclude, int limit) {
        Optional<BaseUserProfile> requesterProfile = baseUserProfileRepository.findByCognitoSub(post.getCognitoSub());
        if (requesterProfile.isEmpty()) return Collections.emptyList();

        List<DatingExtProfile> all = datingExtProfileRepository.findAll();
        return all.stream()
                .filter(p -> !exclude.contains(p.getCognitoSub()))
                .filter(p -> passesDateFilter(p, pref, requesterProfile.get()))
                .limit(limit)
                .map(DatingExtProfile::getCognitoSub)
                .collect(Collectors.toList());
    }

    private List<String> findMatrimonialCandidates(UserPost post, PartnerPreference pref,
                                                    Set<String> exclude, int limit) {
        Optional<BaseUserProfile> requesterProfile = baseUserProfileRepository.findByCognitoSub(post.getCognitoSub());
        if (requesterProfile.isEmpty()) return Collections.emptyList();

        List<MatrimonialExtProfile> all = matrimonialExtProfileRepository.findAll();
        return all.stream()
                .filter(p -> !exclude.contains(p.getCognitoSub()))
                .filter(p -> passesMatrimonialFilter(p, pref, requesterProfile.get()))
                .limit(limit)
                .map(MatrimonialExtProfile::getCognitoSub)
                .collect(Collectors.toList());
    }

    private boolean passesDateFilter(DatingExtProfile candidate, PartnerPreference pref,
                                      BaseUserProfile requester) {
        BaseUserProfile candidateBase = baseUserProfileRepository.findByCognitoSub(candidate.getCognitoSub()).orElse(null);
        if (candidateBase == null) return false;

        // Gender filter
        if (pref.getGenderPref() != null && !pref.getGenderPref().isBlank()
                && !pref.getGenderPref().equalsIgnoreCase("Any")) {
            if (candidateBase.getGender() == null) return false;
            if (!candidateBase.getGender().name().equalsIgnoreCase(pref.getGenderPref())) return false;
        }

        // Age filter
        if (candidateBase.getDateOfBirth() != null) {
            int age = LocalDate.now().getYear() - candidateBase.getDateOfBirth().getYear();
            if (pref.getAgeMin() != null && age < pref.getAgeMin()) return false;
            if (pref.getAgeMax() != null && age > pref.getAgeMax()) return false;
        }

        // Diet filter
        if (pref.getDietaryPref() != null && !pref.getDietaryPref().isBlank()
                && candidate.getDietaryHabits() != null) {
            if (!candidate.getDietaryHabits().equalsIgnoreCase(pref.getDietaryPref())) return false;
        }

        return true;
    }

    private boolean passesMatrimonialFilter(MatrimonialExtProfile candidate, PartnerPreference pref,
                                             BaseUserProfile requester) {
        BaseUserProfile candidateBase = baseUserProfileRepository.findByCognitoSub(candidate.getCognitoSub()).orElse(null);
        if (candidateBase == null) return false;

        // Gender filter
        if (pref.getGenderPref() != null && !pref.getGenderPref().isBlank()
                && !pref.getGenderPref().equalsIgnoreCase("Any")) {
            if (candidateBase.getGender() == null) return false;
            if (!candidateBase.getGender().name().equalsIgnoreCase(pref.getGenderPref())) return false;
        }

        // Age filter
        if (candidateBase.getDateOfBirth() != null) {
            int age = LocalDate.now().getYear() - candidateBase.getDateOfBirth().getYear();
            if (pref.getAgeMin() != null && age < pref.getAgeMin()) return false;
            if (pref.getAgeMax() != null && age > pref.getAgeMax()) return false;
        }

        // Religion filter
        if (pref.getReligionPref() != null && !pref.getReligionPref().isBlank()
                && !pref.getReligionPref().equalsIgnoreCase("Any")
                && candidate.getReligion() != null) {
            if (!candidate.getReligion().equalsIgnoreCase(pref.getReligionPref())) return false;
        }

        // Marital status filter
        if (pref.getMaritalStatusPref() != null && !pref.getMaritalStatusPref().isBlank()
                && candidate.getMaritalStatus() != null) {
            if (!candidate.getMaritalStatus().equalsIgnoreCase(pref.getMaritalStatusPref())) return false;
        }

        // Diet filter
        if (pref.getDietaryPref() != null && !pref.getDietaryPref().isBlank()
                && candidate.getDietaryHabits() != null) {
            if (!candidate.getDietaryHabits().equalsIgnoreCase(pref.getDietaryPref())) return false;
        }

        return true;
    }

    // ── meeting time window ───────────────────────────────────────────────────

    /**
     * Valid window: 11:00 AM – 2:00 AM (next day).
     * Blackout:     2:00 AM – 10:59 AM.
     * Given a candidate time (now + 3h), move forward to 11 AM if in blackout.
     */
    static LocalDateTime nextValidSlot(LocalDateTime from) {
        LocalDateTime candidate = from.plusHours(3);
        int hour = candidate.getHour();
        // blackout: 02:00 to 10:59
        if (hour >= 2 && hour < 11) {
            candidate = candidate.toLocalDate().atTime(11, 0);
        }
        return candidate;
    }
}
