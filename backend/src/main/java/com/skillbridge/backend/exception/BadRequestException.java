package com.skillbridge.backend.exception;

/** Thrown for invalid input or broken business rules (400) and permission issues (403). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
