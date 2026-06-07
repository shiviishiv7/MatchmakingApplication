package com.shiviishiv7.matchmaking.common.exception;

public class MatchmakingException extends RuntimeException {

    private final int statusCode;

    public MatchmakingException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
