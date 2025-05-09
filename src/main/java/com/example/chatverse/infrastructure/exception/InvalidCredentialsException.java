package com.example.chatverse.infrastructure.exception;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}