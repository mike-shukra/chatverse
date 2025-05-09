package com.example.chatverse.infrastructure.exception;

// @ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}