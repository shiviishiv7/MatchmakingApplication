package com.shiviishiv7.matchmaking.provider.vo.ws;

import lombok.Getter;
import lombok.Setter;

/**
 * Sent to /user/{sub}/queue/meeting for all meeting lifecycle events.
 *
 * type values:
 *   WAITING_ROOM      — scheduledAt reached, user should open the call screen
 *   WAITING_FOR_PEER  — user joined waiting room, other peer not yet present
 *   INITIATE_WEBRTC   — both peers present; this user should send the WebRTC OFFER
 *   PEER_READY        — both peers present; this user should wait for the WebRTC OFFER
 */
@Getter
@Setter
public class MeetingNotificationVO {

    private String type;
    private String meetingId;
    private String matchId;
    private String peerUserId;  // cognitoSub of the other participant, present when type=INITIATE_WEBRTC|PEER_READY

    public static MeetingNotificationVO waitingRoom(String meetingId, String matchId) {
        MeetingNotificationVO vo = new MeetingNotificationVO();
        vo.type = "WAITING_ROOM";
        vo.meetingId = meetingId;
        vo.matchId = matchId;
        return vo;
    }

    public static MeetingNotificationVO waitingForPeer(String meetingId, String matchId) {
        MeetingNotificationVO vo = new MeetingNotificationVO();
        vo.type = "WAITING_FOR_PEER";
        vo.meetingId = meetingId;
        vo.matchId = matchId;
        return vo;
    }

    public static MeetingNotificationVO initiateWebRTC(String meetingId, String matchId, String peerUserId) {
        MeetingNotificationVO vo = new MeetingNotificationVO();
        vo.type = "INITIATE_WEBRTC";
        vo.meetingId = meetingId;
        vo.matchId = matchId;
        vo.peerUserId = peerUserId;
        return vo;
    }

    public static MeetingNotificationVO peerReady(String meetingId, String matchId, String peerUserId) {
        MeetingNotificationVO vo = new MeetingNotificationVO();
        vo.type = "PEER_READY";
        vo.meetingId = meetingId;
        vo.matchId = matchId;
        vo.peerUserId = peerUserId;
        return vo;
    }
}
