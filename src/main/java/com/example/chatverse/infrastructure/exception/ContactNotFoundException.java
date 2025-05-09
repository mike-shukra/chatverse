package com.example.chatverse.infrastructure.exception;

public class ContactNotFoundException extends ResourceNotFoundException {
    public ContactNotFoundException(String message) {
        super(message);
    }
}