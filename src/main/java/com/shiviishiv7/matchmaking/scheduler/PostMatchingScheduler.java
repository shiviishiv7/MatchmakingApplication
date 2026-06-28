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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Runs every 15 minutes.
 * Matches active posts against other active posts with the same intent.
 * Creates a Zoom meeting for each match and emails both users the join link.
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
        // Optional partner preferences — if not provided, match on intent alone
        Optional<PartnerPreference> prefOpt = partnerPreferenceRepository.findByPostId(post.getId());
        PartnerPreference pref = prefOpt.orElse(null);

        // Users already matched for this post
        Set<String> alreadyMatched = matchResultRepository
                .findByPostId(post.getId())
                .stream()
                .map(MatchResult::getCognitoSubB)
                .collect(Collectors.toSet());
        alreadyMatched.add(post.getCognitoSub());

        int slotsLeft = MAX_MATCHES_PER_POST - post.getMatchCount();
        if (slotsLeft <= 0) {
            post.setStatus(PostStatus.CLOSED);
            userPostRepository.save(post);
            return;
        }

        List<String> candidates = findCandidates(post, pref, alreadyMatched, slotsLeft);
        if (candidates.isEmpty()) {
            log.info("No new candidates for post {} (intent={})", post.getId(), post.getIntent());
            return;
        }

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

    // ── candidate discovery — post-to-post matching ───────────────────────────

    private List<String> findCandidates(UserPost post, PartnerPreference pref,
                                         Set<String> exclude, int limit) {
        // Find all OTHER active posts with the same intent
        List<UserPost> otherPosts = userPostRepository.findActivePostsForMatching(LocalDateTime.now())
                .stream()
                .filter(p -> !p.getCognitoSub().equals(post.getCognitoSub()))
                .filter(p -> p.getIntent() == post.getIntent())
                .filter(p -> !exclude.contains(p.getCognitoSub()))
                .collect(Collectors.toList());

        // Deduplicate by user (one user may have multiple posts — pick the first)
        Map<String, UserPost> byUser = new LinkedHashMap<>();
        for (UserPost p : otherPosts) {
            byUser.putIfAbsent(p.getCognitoSub(), p);
        }

        return byUser.values().stream()
                .filter(candidatePost -> passesPreferenceFilter(post, pref, candidatePost))
                .limit(limit)
                .map(UserPost::getCognitoSub)
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the candidatePost's owner is acceptable given the
     * post-owner's partner preferences (and vice versa for gender).
     */
    private boolean passesPreferenceFilter(UserPost post, PartnerPreference pref,
                                            UserPost candidatePost) {
        // No preferences set → accept all same-intent posts
        if (pref == null) return true;

        Optional<BaseUserProfile> candidateProfileOpt =
                baseUserProfileRepository.findByCognitoSub(candidatePost.getCognitoSub());
        if (candidateProfileOpt.isEmpty()) return true; // no profile yet → don't block

        BaseUserProfile candidateProfile = candidateProfileOpt.get();

        // Gender preference
        if (pref.getGenderPref() != null && !pref.getGenderPref().isBlank()
                && !pref.getGenderPref().equalsIgnoreCase("Any")) {
            if (candidateProfile.getGender() != null &&
                !candidateProfile.getGender().name().equalsIgnoreCase(pref.getGenderPref())) {
                return false;
            }
        }

        // Age preference (uses dateOfBirth from BaseUserProfile)
        if (candidateProfile.getDateOfBirth() != null) {
            int age = java.time.LocalDate.now().getYear() - candidateProfile.getDateOfBirth().getYear();
            if (pref.getAgeMin() != null && age < pref.getAgeMin()) return false;
            if (pref.getAgeMax() != null && age > pref.getAgeMax()) return false;
        }

        // Religion preference (matrimonial) — check if candidate's post mentions religion
        // For now we check the candidate's partner prefs to ensure mutual compatibility
        Optional<PartnerPreference> candidatePrefOpt =
                partnerPreferenceRepository.findByPostId(candidatePost.getId());
        if (candidatePrefOpt.isPresent()) {
            PartnerPreference candidatePref = candidatePrefOpt.get();
            // Check candidate's gender preference accepts the post owner
            Optional<BaseUserProfile> ownerProfile =
                    baseUserProfileRepository.findByCognitoSub(post.getCognitoSub());
            if (ownerProfile.isPresent() && candidatePref.getGenderPref() != null
                    && !candidatePref.getGenderPref().isBlank()
                    && !candidatePref.getGenderPref().equalsIgnoreCase("Any")) {
                BaseUserProfile owner = ownerProfile.get();
                if (owner.getGender() != null &&
                    !owner.getGender().name().equalsIgnoreCase(candidatePref.getGenderPref())) {
                    return false;
                }
            }
        }

        return true;
    }

    // ── schedule match + Zoom ─────────────────────────────────────────────────

    private void scheduleMatch(UserPost post, String candidateSub, LocalDateTime scheduledAt) {
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

        String topic = "Shall We Connect — " +
                (post.getIntent() == IntentType.DATING ? "Dating" : "Matrimonial") + " Meeting";
        ZoomService.ZoomMeetingResult zoom = zoomService.createMeeting(topic, scheduledAt, MEETING_DURATION_MINUTES);

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

        String formattedTime = scheduledAt.format(EMAIL_DT);
        sendMatchEmails(post.getCognitoSub(), candidateSub, zoom.getJoinUrl(), formattedTime);

        log.info("Matched post {} (user {}) with candidate {} → Zoom {} at {}",
                post.getId(), post.getCognitoSub(), candidateSub, zoom.getMeetingId(), scheduledAt);
    }

    private void sendMatchEmails(String subA, String subB, String joinUrl, String formattedTime) {
        Optional<BaseUserProfile> profileA = baseUserProfileRepository.findByCognitoSub(subA);
        Optional<BaseUserProfile> profileB = baseUserProfileRepository.findByCognitoSub(subB);

        profileA.ifPresent(a -> profileB.ifPresent(b -> {
            emailService.sendMeetingScheduledEmail(a.getEmail(), a.getName(), b.getName(), joinUrl, formattedTime);
            emailService.sendMeetingScheduledEmail(b.getEmail(), b.getName(), a.getName(), joinUrl, formattedTime);
        }));
    }

    // ── meeting time window ───────────────────────────────────────────────────

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
