package com.shiviishiv7.matchmaking.service.match;

import com.shiviishiv7.matchmaking.common.enums.MatchStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.common.enums.MeetingType;
import com.shiviishiv7.matchmaking.provider.implementation.BaseUserProfileRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.model.profile.BaseUserProfile;
import com.shiviishiv7.matchmaking.provider.vo.ws.MatchNotificationVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.MeetingNotificationVO;
import com.shiviishiv7.matchmaking.service.email.EmailService;
import com.shiviishiv7.matchmaking.service.pool.UserPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles the post-match online check and WebRTC connection trigger.
 *
 * Responsibilities:
 *  1. connectNextOnlineMatch  — scan PENDING matches for a user, find the first online
 *                               candidate, create an INSTANT meeting and open the waiting room.
 *  2. notifyWaitingMatchersOnJoin — when a new user joins the pool, check if any waiting
 *                               user has a PENDING match with them and connect immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchConnectService {

    private static final String QUEUE_MEETING = "/queue/meeting";
    private static final String QUEUE_MATCHES = "/queue/matches";

    private final MatchResultRepository matchResultRepository;
    private final MeetingRepository meetingRepository;
    private final UserPoolService userPoolService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BaseUserProfileRepository userProfileRepository;
    private final EmailService emailService;

    /**
     * Scans PENDING MatchResults for the given user (best score first).
     * Connects the first candidate who is currently online and not busy.
     *
     * @return true if a connection was initiated, false if no online candidate found.
     */
    @Transactional
    public boolean connectNextOnlineMatch(String cognitoSubA) {
        List<MatchResult> pending = matchResultRepository.findPendingMatchesForUser(cognitoSubA);
        log.info("connectNextOnlineMatch: {} has {} pending matches", cognitoSubA, pending.size());

        for (MatchResult match : pending) {
            String candidateSub = match.getCognitoSubB();
            if (!userPoolService.isInPool(candidateSub)) {
                log.debug("Candidate {} is offline — skipping", candidateSub);
                continue;
            }
            if (userPoolService.isBusy(candidateSub)) {
                log.debug("Candidate {} is busy — skipping", candidateSub);
                continue;
            }

            // Both online and available — create INSTANT meeting in WAITING_ROOM state
            Meeting meeting = Meeting.builder()
                    .matchResultId(match.getId())
                    .roundNumber(match.getRoundCount() + 1)
                    .scheduledAt(LocalDateTime.now())
                    .meetingType(MeetingType.INSTANT)
                    .status(MeetingStatus.WAITING_ROOM)
                    .durationMinutes(30)
                    .build();
            meeting = meetingRepository.save(meeting);

            match.setStatus(MatchStatus.MEETING_SCHEDULED);
            matchResultRepository.save(match);

            // Push WAITING_ROOM notification to both — existing waiting room controller takes over
            MeetingNotificationVO notification = MeetingNotificationVO.waitingRoom(
                    meeting.getId().toString(), match.getId().toString());
            messagingTemplate.convertAndSendToUser(cognitoSubA, QUEUE_MEETING, notification);
            messagingTemplate.convertAndSendToUser(candidateSub, QUEUE_MEETING, notification);

            log.info("Instant meeting {} created for match {} ({} ↔ {})",
                    meeting.getId(), match.getId(), cognitoSubA, candidateSub);
            return true;
        }

        log.info("connectNextOnlineMatch: no online candidate found for {}", cognitoSubA);
        return false;
    }

    /**
     * Called when a new user joins the WebSocket pool.
     * Checks if any waiting user has a PENDING match with this new joiner.
     * If the waiting user is also online, connects them immediately.
     */
    @Transactional
    public void notifyWaitingMatchersOnJoin(String newUserSub) {
        List<MatchResult> waitingMatches = matchResultRepository.findPendingByCandidateSub(newUserSub);
        for (MatchResult match : waitingMatches) {
            String waiterSub = match.getCognitoSubA();
            if (!userPoolService.isInPool(waiterSub)) continue;

            log.info("New joiner {} satisfies pending match for {} — connecting", newUserSub, waiterSub);
            boolean connected = connectNextOnlineMatch(waiterSub);
            if (connected) {
                // Also push a heads-up to the waiter's matches queue so the UI can react
                messagingTemplate.convertAndSendToUser(waiterSub, QUEUE_MATCHES,
                        MatchNotificationVO.builder()
                                .event("MATCH_NOW_ONLINE")
                                .matchedUserId(newUserSub)
                                .message("Your match just came online! Connecting now...")
                                .build());
                break; // one connection at a time per new joiner
            }
        }
    }

    /**
     * Sends email to both sides when no online candidate is found at match time.
     * User A learns their match is saved; User B learns someone is waiting.
     */
    public void sendNoOnlineMatchEmails(String cognitoSubA, String cognitoSubB) {
        BaseUserProfile userA = userProfileRepository.findByCognitoSub(cognitoSubA).orElse(null);
        BaseUserProfile userB = userProfileRepository.findByCognitoSub(cognitoSubB).orElse(null);

        if (userA != null && userB != null) {
            emailService.sendMatchSavedEmail(userA.getEmail(), userA.getName(), userB.getName());
            emailService.sendMatchWaitingEmail(userB.getEmail(), userB.getName(), userA.getName());
        } else {
            log.warn("Could not send match emails — profile missing for {} or {}", cognitoSubA, cognitoSubB);
        }
    }
}
