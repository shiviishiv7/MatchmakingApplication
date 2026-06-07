package com.shiviishiv7.matchmaking.common.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {

    private String error;
    private String message;
    private String details;
    private int status;

    public ErrorResponse(String error, String message, int status) {
        this.error = error;
        this.message = message;
        this.details = null;
        this.status = status;
    }
}
