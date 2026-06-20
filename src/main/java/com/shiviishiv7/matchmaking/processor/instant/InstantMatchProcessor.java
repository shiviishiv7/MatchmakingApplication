package com.shiviishiv7.matchmaking.processor.instant;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.MatchRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserPreferenceRepository;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
import com.shiviishiv7.matchmaking.provider.model.User;
import com.shiviishiv7.matchmaking.provider.model.UserPreference;
import com.shiviishiv7.matchmaking.provider.vo.ws.MatchNotificationVO;
import com.shiviishiv7.matchmaking.service.presence.UserPresenceService;
import com.shiviishiv7.matchmaking.service.waiting.WaitingQueueService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.DATA_NOT_FOUND;
import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.UNKNOWN_EXCEPTION;

@Component
@Transactional
@Slf4j
public class InstantMatchProcessor implements IInstantMatchProcessor {

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private WaitingQueueService waitingQueueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void startLooking(String userId) throws MatchmakingException {
        try {
            log.info("User {} started looking for an instant match.", userId);
//            messagingTemplate.convertAndSendToUser(userSub, "/queue/match", notification);

            Optional<User> optionalUser = userRepository.findById(Integer.valueOf(userId));
            if (optionalUser.isEmpty()) {
                log.error("ALERT_FOR_ERROR: User not found for ID: {}", userId);
                throw new MatchmakingException("User does not exist", DATA_NOT_FOUND);
            }
            User currentUser = optionalUser.get();

            // Mark as looking so others can find this user too
            userPresenceService.markAsLooking(userId);

            Optional<UserPreference> myPrefsOpt = userPreferenceRepository.findByUserId(userId);

            // Find a compatible online user
            Set<String> lookingUserIds = userPresenceService.getAllLookingUserIds();
            User bestCandidate = null;
            double bestScore = -1;

            for (String candidateIdStr : lookingUserIds) {
                UUID candidateId = UUID.fromString(candidateIdStr);
                if (candidateId.equals(userId)) continue;

                Optional<User> candidateOpt = userRepository.findById(candidateId);
                if (candidateOpt.isEmpty()) continue;
                User candidate = candidateOpt.get();

                // Skip if already matched before
                if (matchRepository.existsByCognitoSubAAndCognitoSubB(userId, candidateOpt.get().getCognitoSub())) continue;

                Optional<UserPreference> theirPrefsOpt = userPreferenceRepository.findByUserId(String.valueOf(candidateId));

                if (!isCompatible(currentUser, myPrefsOpt.orElse(null),
                                  candidate, theirPrefsOpt.orElse(null))) continue;

                double score = computeScore(currentUser, myPrefsOpt.orElse(null),
                                            candidate, theirPrefsOpt.orElse(null));
                if (score > bestScore) {
                    bestScore = score;
                    bestCandidate = candidate;
                }
            }

            if (bestCandidate != null) {
                log.info("Instant match found between {} and {} with score {}.", userId, bestCandidate.getId(), bestScore);
                createInstantMatchAndNotify(currentUser, bestCandidate, bestScore);
            } else {
                log.info("No instant match found for user {}. Adding to waiting queue.", userId);
                waitingQueueService.enqueue(userId);
                notifyUser(userId.toString(), MatchNotificationVO.builder()
                        .event("NO_MATCH_FOUND")
                        .message("No one is available right now. We'll notify you as soon as someone compatible comes online.")
                        .build());
            }

        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error in startLooking for user {}. Error: {}", userId, ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while searching for a match: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void stopLooking(String userId) throws MatchmakingException {
        try {
            log.info("User {} stopped looking for a match.", userId);
            userPresenceService.markAsNotLooking(userId);
            waitingQueueService.dequeue(userId);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error in stopLooking for user {}. Error: {}", userId, ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while stopping match search: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public void v2(String sub) {
        messagingTemplate.convertAndSendToUser(sub, "/queue/match", "hi hello");
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void createInstantMatchAndNotify(User userA, User userB, double score) {
        // Remove both from looking / waiting
        userPresenceService.markAsNotLooking(String.valueOf(userA.getId()));
        userPresenceService.markAsNotLooking(String.valueOf(userB.getId()));
        waitingQueueService.dequeue(String.valueOf(userA.getId()));
        waitingQueueService.dequeue(String.valueOf(userB.getId()));

        Match match = Match.builder()
                .cognitoSubA(userA.getCognitoSub())
                .cognitoSubB(userB.getCognitoSub())
                .status(MatchStatus.MEETING_SCHEDULED)
                .compatibilityScore(score)
                .meetingType(MeetingType.INSTANT)
                .roundCount(1)
                .maxRounds(3)
                .build();
        match = matchRepository.save(match);

        String matchId = match.getId().toString();

        notifyUser(userA.getId().toString(), MatchNotificationVO.builder()
                .event("MATCH_FOUND")
                .matchId(matchId)
                .matchedUserId(userB.getId().toString())
                .matchedUserName(userB.getFirstName() + " " + userB.getLastName())
//                .matchedUserProfilePic(userB.getProfilePictureUrl())
                .compatibilityScore(score)
                .message("Great news! We found a match. Your meeting is starting now.")
                .build());

        notifyUser(userB.getId().toString(), MatchNotificationVO.builder()
                .event("MATCH_FOUND")
                .matchId(matchId)
                .matchedUserId(userA.getId().toString())
                .matchedUserName(userA.getFirstName() + " " + userA.getLastName())
//                .matchedUserProfilePic(userA.getProfilePictureUrl())
                .compatibilityScore(score)
                .message("Great news! We found a match. Your meeting is starting now.")
                .build());

        log.info("Match created and both users notified. Match ID: {}", matchId);
    }

    private void notifyUser(String userSub, MatchNotificationVO notification) {
        messagingTemplate.convertAndSendToUser(userSub, "/queue/match", notification);
    }

    /**
     * Bidirectional compatibility check — both users must satisfy each other's preferences.
     */
    private boolean isCompatible(User a, UserPreference aPrefs, User b, UserPreference bPrefs) {
        int ageA = computeAge(a);
        int ageB = computeAge(b);

        // Check A's preferences against B
        if (aPrefs != null) {
            if (aPrefs.getPreferredGender() != null && !aPrefs.getPreferredGender().equals(b.getGender())) return false;
            if (ageB < aPrefs.getMinAge() || ageB > aPrefs.getMaxAge()) return false;
//            if (!aPrefs.getPreferredIndustries().isEmpty()
//                    && b.getIndustry() != null
//                    && !aPrefs.getPreferredIndustries().contains(b.getIndustry())) return false;
////            if (Boolean.FALSE.equals(aPrefs.getSameCompanyAllowed())
////                    && a.getCompany() != null && b.getCompany() != null
////                    && a.getCompany().getId().equals(b.getCompany().getId())) return false;
//            if (timezoneOffsetHours(a, b) > aPrefs.getMaxTimezoneOffsetHours()) return false;
        }

        // Check B's preferences against A
        if (bPrefs != null) {
            if (bPrefs.getPreferredGender() != null && !bPrefs.getPreferredGender().equals(a.getGender())) return false;
            if (ageA < bPrefs.getMinAge() || ageA > bPrefs.getMaxAge()) return false;
//            if (!bPrefs.getPreferredIndustries().isEmpty()
//                    && a.getIndustry() != null
//                    && !bPrefs.getPreferredIndustries().contains(a.getIndustry())) return false;
////            if (Boolean.FALSE.equals(bPrefs.getSameCompanyAllowed())
////                    && a.getCompany() != null && b.getCompany() != null
////                    && a.getCompany().getId().equals(b.getCompany().getId())) return false;
//            if (timezoneOffsetHours(a, b) > bPrefs.getMaxTimezoneOffsetHours()) return false;
        }

        return true;
    }

    /**
     * Scores a candidate 0.0–1.0. Higher = better fit.
     * Currently based on timezone proximity and industry match.
     */
    private double computeScore(User a, UserPreference aPrefs, User b, UserPreference bPrefs) {
        double score = 0.5; // baseline

        int tzOffset = timezoneOffsetHours(a, b);
        if (tzOffset == 0) score += 0.3;
        else if (tzOffset <= 2) score += 0.2;
        else if (tzOffset <= 4) score += 0.1;

//        if (a.getIndustry() != null && a.getIndustry().equals(b.getIndustry())) score += 0.2;

        return Math.min(score, 1.0);
    }

    private int computeAge(User user) {
        if (user.getDateOfBirth() == null) return 0;
        return Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
    }

    private int timezoneOffsetHours(User a, User b) {
        try {
//            if (a.getTimezone() == null || b.getTimezone() == null) return 0;
//            ZoneOffset offsetA = ZoneId.of(a.getTimezone()).getRules().getStandardOffset(java.time.Instant.now());
//            ZoneOffset offsetB = ZoneId.of(b.getTimezone()).getRules().getStandardOffset(java.time.Instant.now());
//            return Math.abs((offsetA.getTotalSeconds() - offsetB.getTotalSeconds()) / 3600);
            return 100;
        } catch (Exception ex) {
            return 0;
        }
    }
}
