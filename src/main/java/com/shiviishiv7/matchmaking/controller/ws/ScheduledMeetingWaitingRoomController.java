package com.shiviishiv7.matchmaking.controller.ws;

import com.shiviishiv7.matchmaking.common.enums.MeetingStatus;
import com.shiviishiv7.matchmaking.provider.implementation.MeetingRepository;
import com.shiviishiv7.matchmaking.provider.model.Match;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles the waiting room phase for scheduled WebRTC meetings.
 *
 * Flow:
 *   1. Scheduler opens the waiting room (status → WAITING_ROOM, notifies both users)
 *   2. Each user opens the call screen and sends a join-waiting-room message here
 *   3. When both users have joined:
 *      - userA receives INITIATE_WEBRTC → sends an OFFER via /app/webrtc/signal
 *      - userB receives PEER_READY     → waits for the OFFER
 *   4. From here on the standard WebRTCSignalingController relay handles the call
 *
 * Client sends to:  /app/meeting/join-waiting-room
 * Server pushes to: /user/{sub}/queue/meeting
 */
@Controller
@Slf4j
public class ScheduledMeetingWaitingRoomController {

    private static final String WAITING_KEY_PREFIX = "meeting:waiting:";
    // TTL matches typical max meeting duration — cleans up if both users never joined
    private static final long WAITING_ROOM_TTL_MINUTES = 60;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @MessageMapping("/meeting/join-waiting-room")
    @Transactional
    public void joinWaitingRoom(@Payload WaitingRoomJoinVO payload) {
        String meetingId = payload.getMeetingId();
        String joiningUserSub = payload.getUserId();

        if (meetingId == null || joiningUserSub == null) {
            log.warn("WaitingRoom: missing meetingId or userId — dropping.");
            return;
        }

        Optional<Meeting> optionalMeeting = meetingRepository.findById(UUID.fromString(meetingId));
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

        Match match = meeting.getMatch();
        String subA = match.getUserA().getCognitoSub();
        String subB = match.getUserB().getCognitoSub();
        String matchId = match.getId().toString();

        // Record this user as present in Redis
        String redisKey = WAITING_KEY_PREFIX + meetingId;
        redisTemplate.opsForSet().add(redisKey, joiningUserSub);
        redisTemplate.expire(redisKey, WAITING_ROOM_TTL_MINUTES, TimeUnit.MINUTES);

        Set<String> presentUsers = redisTemplate.opsForSet().members(redisKey);
        boolean bothPresent = presentUsers != null && presentUsers.contains(subA) && presentUsers.contains(subB);

        if (!bothPresent) {
            // Only one peer so far — tell them to wait
            messagingTemplate.convertAndSendToUser(
                    joiningUserSub,
                    "/queue/meeting",
                    MeetingNotificationVO.waitingForPeer(meetingId, matchId)
            );
            log.info("WaitingRoom: {} joined meeting {}, waiting for peer.", joiningUserSub, meetingId);
            return;
        }

        // Both peers present — transition meeting to IN_PROGRESS and kick off signaling
        meeting.setStatus(MeetingStatus.IN_PROGRESS);
        meetingRepository.save(meeting);
        redisTemplate.delete(redisKey);

        log.info("WaitingRoom: both peers present for meeting {} — starting WebRTC. Initiator: {}", meetingId, subA);

        // userA is the designated initiator — they send the OFFER
        messagingTemplate.convertAndSendToUser(
                subA,
                "/queue/meeting",
                MeetingNotificationVO.initiateWebRTC(meetingId, matchId, subB)
        );

        // userB waits for the OFFER from userA
        messagingTemplate.convertAndSendToUser(
                subB,
                "/queue/meeting",
                MeetingNotificationVO.peerReady(meetingId, matchId, subA)
        );
    }
}
