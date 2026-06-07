package com.shiviishiv7.matchmaking.common.enums;


public enum MatchStatus {
    PENDING,            // match created, first meeting not yet scheduled
    MEETING_SCHEDULED,  // zoom link sent, waiting for meeting
    AWAITING_FEEDBACK,  // meeting done, waiting for both feedbacks
    ANOTHER_ROUND,      // both agreed to one more round
    COMPLETED,          // both liked — phone numbers shared
    ENDED               // one or both not interested
}

