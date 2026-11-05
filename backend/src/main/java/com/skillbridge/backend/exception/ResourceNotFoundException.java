package com.skillbridge.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when data cannot be found; mapped to 404 with a friendly message. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
