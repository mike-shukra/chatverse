package com.example.chatverse.infrastructure.exception;

public class UserNotAuthenticatedException extends AppException {
    public UserNotAuthenticatedException(String message) {
        super(message);
    }

    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
