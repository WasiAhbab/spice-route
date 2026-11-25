package com.spiceroute.common;

import java.net.http.HttpTimeoutException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {

    private static final Logger log = LoggerFactory.getLogger(Errors.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message",
            exception.getReason() == null ? "Request rejected" : exception.getReason()));
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    ResponseEntity<?> stale() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "This record changed. Refresh before editing again."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "This record conflicts with an existing record."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message",
            exception.getMessage() == null ? "Invalid input" : exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> malformed() {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid request body"));
    }

    @ExceptionHandler(HttpTimeoutException.class)
    ResponseEntity<?> timeout() {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(Map.of("message", "AI analysis timed out. Please try again."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception exception) {
        // Do not log provider responses, credentials, SQL bind parameters or customer data.
        log.error("Unhandled request failure: {}", exception.getClass().getName());
        return ResponseEntity.internalServerError().body(Map.of("message", "Unexpected server error"));
    }
}
