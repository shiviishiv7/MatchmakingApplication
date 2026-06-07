package com.shiviishiv7.matchmaking.common.enums;


public enum UserStatus {
    PENDING_VERIFICATION,   // signed up, email not yet verified
    INCOMPLETE_PROFILE,     // verified, profile not fully filled
    IN_POOL,                // ready to be matched
    MATCHED,                // currently in an active match
    COMPLETED,              // had a successful match (shared number)
    EXITED                  // left the platform voluntarily
}

