package com.shiviishiv7.matchmaking.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MatchmakingException.class)
    public ResponseEntity<ErrorResponse> handleMatchmakingException(MatchmakingException ex) {
        log.error("ALERT_FOR_ERROR: MatchmakingException — status: {}, message: {}", ex.getStatusCode(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "Business Exception",
                ex.getMessage().toLowerCase(Locale.ROOT),
                ex.getStatusCode());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("ALERT_FOR_ERROR: Unexpected exception — {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal Server Error",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
