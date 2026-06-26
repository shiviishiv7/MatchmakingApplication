package com.shiviishiv7.matchmaking.common.enums;

public enum MatchStatus {
    PENDING,            // match created, first meeting not yet scheduled
    MEETING_SCHEDULED,  // meeting is booked, waiting for meeting day
    AWAITING_FEEDBACK,  // meeting done, waiting for both users to respond
    ANOTHER_ROUND,      // both said YES, next meeting being scheduled
    COMPLETED,          // all rounds done, both kept saying YES
    ENDED               // at least one user said NO
}
