package com.shiviishiv7.matchmaking.processor.instant;

import com.shiviishiv7.matchmaking.common.enums.Gender;
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
import java.util.Optional;
import java.util.Set;


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

            Optional<UserPreference> myPrefsOpt = userPreferenceRepository.findByCognitoSub(currentUser.getCognitoSub());

            // Find a compatible online user
            Set<String> lookingUserIds = userPresenceService.getAllLookingUserIds();
            User bestCandidate = null;
            double bestScore = -1;

            for (String candidateIdStr : lookingUserIds) {
                if (candidateIdStr.equals(userId)) continue;

                Optional<User> candidateOpt = userRepository.findById(Integer.valueOf(candidateIdStr));
                if (candidateOpt.isEmpty()) continue;
                User candidate = candidateOpt.get();

                // Skip if already matched before
                if (matchRepository.existsByCognitoSubAAndCognitoSubB(currentUser.getCognitoSub(), candidate.getCognitoSub())) continue;

                Optional<UserPreference> theirPrefsOpt = userPreferenceRepository.findByCognitoSub(candidate.getCognitoSub());

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

        boolean sameCompany = a.getCompanyId() != null
                && a.getCompanyId().equals(b.getCompanyId());

        // Check A's preferences against B
        if (aPrefs != null) {
            if (aPrefs.getPreferredGender() != null && !aPrefs.getPreferredGender().equals(b.getGender())) return false;
            if (ageB < aPrefs.getMinAge() || ageB > aPrefs.getMaxAge()) return false;
            if (Boolean.FALSE.equals(aPrefs.getSameCompanyAllowed()) && sameCompany) return false;
        }

        // Check B's preferences against A
        if (bPrefs != null) {
            if (bPrefs.getPreferredGender() != null && !bPrefs.getPreferredGender().equals(a.getGender())) return false;
            if (ageA < bPrefs.getMinAge() || ageA > bPrefs.getMaxAge()) return false;
            if (Boolean.FALSE.equals(bPrefs.getSameCompanyAllowed()) && sameCompany) return false;
        }

        return true;
    }

    /**
     * Scores a candidate 0.0–1.0. Higher = better fit.
     *
     * Weight breakdown (must sum to 1.0):
     *   Baseline .............. 0.2  (every compatible pair gets this)
     *   Gender preference ..... 0.4  (0.2 per direction — A→B and B→A)
     *   Age proximity ......... 0.4  (0.2 per direction — A's pref for B and B's pref for A)
     *
     * To add a new signal in the future (e.g. location, company):
     *   1. Add a private scorer method returning 0.0–1.0
     *   2. Add a weighted line below, redistributing weights to keep total = 1.0
     *   3. Add the hard-rejection check in isCompatible() if needed
     */
    private double computeScore(User a, UserPreference aPrefs, User b, UserPreference bPrefs) {
        double score = 0.2; // baseline

        // Gender preference (0.2 per direction = 0.4 total)
        score += genderScore(b.getGender(), aPrefs) * 0.2;
        score += genderScore(a.getGender(), bPrefs) * 0.2;

        // Age proximity (0.2 per direction = 0.4 total)
        score += ageProximityScore(computeAge(b), aPrefs) * 0.2;
        score += ageProximityScore(computeAge(a), bPrefs) * 0.2;

        // Future signals go here, e.g.:
        // score += locationScore(a, b, aPrefs) * 0.X;

        return Math.min(score, 1.0);
    }

    // Returns 1.0 if gender matches preference, 0.5 if no preference set, 0.0 if mismatch.
    private double genderScore(Gender candidateGender, UserPreference prefs) {
        if (prefs == null || prefs.getPreferredGender() == null) return 0.5;
        return prefs.getPreferredGender().equals(candidateGender) ? 1.0 : 0.0;
    }

    // Returns 0.0–1.0: 1.0 = age at center of preferred range, 0.0 = at boundary, 0.5 = no preference.
    private double ageProximityScore(int age, UserPreference prefs) {
        if (prefs == null) return 0.5;
        int min = prefs.getMinAge() != null ? prefs.getMinAge() : 18;
        int max = prefs.getMaxAge() != null ? prefs.getMaxAge() : 60;
        if (max <= min) return 0.5;
        double center = (min + max) / 2.0;
        double halfRange = (max - min) / 2.0;
        return Math.max(0.0, 1.0 - (Math.abs(age - center) / halfRange));
    }

    private int computeAge(User user) {
        if (user.getDateOfBirth() != null) {
            return Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        }
        return user.getAge();
    }
}
