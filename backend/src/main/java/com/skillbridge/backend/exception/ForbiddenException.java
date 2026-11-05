package com.skillbridge.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user lacks permission for an action; mapped to 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
