package com.shiviishiv7.matchmaking.controller;

import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MatchResultRepository;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.MatchResult;
import com.shiviishiv7.matchmaking.provider.model.Meeting;
import com.shiviishiv7.matchmaking.provider.vo.ws.MeetingNotificationVO;
import com.shiviishiv7.matchmaking.provider.vo.ws.WaitingRoomJoinVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Handles the waiting room phase for scheduled WebRTC meetings.
 *
 * Flow:
 *   1. MeetingSchedulerJob opens the waiting room (status → WAITING_ROOM, notifies both users)
 *   2. Each user opens the call screen → client sends join-waiting-room message here
 *   3. When both users have joined:
 *      - subA receives INITIATE_WEBRTC → sends the WebRTC OFFER
 *      - subB receives PEER_READY     → waits for the OFFER
 *   4. Standard WebRTCSignalingController relay handles the call from here
 *
 * Client sends to:  /app/meeting/join-waiting-room
 * Server pushes to: /user/{cognitoSub}/queue/meeting
 *
 * BUG FIXED (original code):
 *   Line: String subB = match.getCognitoSubA();  ← should be getCognitoSubB()
 *   This caused both WebRTC start signals to go to subA, so subB never received
 *   the PEER_READY notification and the call never started.
 */
@Controller
@Slf4j
public class ScheduledMeetingWaitingRoomController {

    private static final String WAITING_KEY_PREFIX = "meeting:waiting:";
    private static final long   WAITING_ROOM_TTL_MINUTES = 60;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MatchResultRepository matchRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MessageMapping("/meeting/join-waiting-room")
    @Transactional
    public void joinWaitingRoom(@Payload WaitingRoomJoinVO payload) {
        String meetingId      = payload.getMeetingId();
        String joiningUserSub = payload.getUserId();

        if (meetingId == null || joiningUserSub == null) {
            log.warn("WaitingRoom: missing meetingId or userId — dropping.");
            return;
        }

        // ── Resolve meeting ───────────────────────────────────────────────────
        Optional<Meeting> optionalMeeting = meetingRepository.findById(Integer.valueOf(meetingId));
        if (optionalMeeting.isEmpty()) {
            log.warn("WaitingRoom: meeting not found for ID: {}", meetingId);
            return;
        }
        Meeting meeting = optionalMeeting.get();

        if (meeting.getStatus() != MeetingStatus.WAITING_ROOM) {
            log.warn("WaitingRoom: meeting {} is not in WAITING_ROOM state (current: {}) — ignoring join from {}.",
                    meetingId, meeting.getStatus(), joiningUserSub);
            return;
        }

        // ── Resolve match ─────────────────────────────────────────────────────
        Optional<MatchResult> optionalMatch = matchRepository.findById(
                Integer.valueOf(meeting.getMatchId()));
        if (optionalMatch.isEmpty()) {
            log.error("ALERT_FOR_ERROR: MatchResult not found for matchId: {} on meeting: {}",
                    meeting.getMatchId(), meetingId);
            return;
        }
        MatchResult match = optionalMatch.get();
        String subA   = match.getCognitoSubA();
        String subB   = match.getCognitoSubB();   // BUG FIX: was getCognitoSubA() in original
        String matchId = match.getId().toString();

        // ── Track who has joined in Redis ─────────────────────────────────────
        String redisKey = WAITING_KEY_PREFIX + meetingId;
        redisTemplate.opsForSet().add(redisKey, joiningUserSub);
        redisTemplate.expire(redisKey, WAITING_ROOM_TTL_MINUTES, TimeUnit.MINUTES);

        Set<String> presentUsers = redisTemplate.opsForSet().members(redisKey);
        boolean bothPresent = presentUsers != null
                && presentUsers.contains(subA)
                && presentUsers.contains(subB);

        if (!bothPresent) {
            // Only first peer — tell them to wait
            messagingTemplate.convertAndSendToUser(
                    joiningUserSub,
                    "/queue/meeting",
                    MeetingNotificationVO.waitingForPeer(meetingId, matchId)
            );
            log.info("WaitingRoom: {} joined meeting {}, waiting for peer.", joiningUserSub, meetingId);
            return;
        }

        // ── Both present — start WebRTC ───────────────────────────────────────
        meeting.setStatus(MeetingStatus.IN_PROGRESS);
        meetingRepository.save(meeting);
        redisTemplate.delete(redisKey);

        log.info("WaitingRoom: both peers present for meeting {} — starting WebRTC. Initiator: {}",
                meetingId, subA);

        // subA is designated initiator — sends the SDP OFFER
        messagingTemplate.convertAndSendToUser(
                subA,
                "/queue/meeting",
                MeetingNotificationVO.initiateWebRTC(meetingId, matchId, subB)
        );

        // subB waits for the OFFER from subA
        messagingTemplate.convertAndSendToUser(
                subB,
                "/queue/meeting",
                MeetingNotificationVO.peerReady(meetingId, matchId, subA)
        );
    }
}
