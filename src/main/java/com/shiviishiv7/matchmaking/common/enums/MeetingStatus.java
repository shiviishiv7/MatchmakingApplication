package com.shiviishiv7.matchmaking.common.enums;



public enum MeetingStatus {
    SCHEDULED,      // meeting created, waiting for scheduledAt day
    WAITING_ROOM,   // scheduledAt reached, waiting for both users to join
    IN_PROGRESS,    // WebRTC call is live between both peers
    COMPLETED,      // call ended, moving to feedback
    CANCELLED       // cancelled before it happened
}
