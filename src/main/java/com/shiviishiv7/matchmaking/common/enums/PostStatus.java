package com.shiviishiv7.matchmaking.common.enums;

public enum PostStatus {
    ACTIVE,
    CLOSED,   // manually closed by user or 5 matches reached
    EXPIRED   // 30 days passed with no/partial matches
}
