package com.example.chatverse.presentation.exception;

public class InvalidTokenUserIdException extends AppException{
    public InvalidTokenUserIdException(String message) {
        super(message);
    }

    public InvalidTokenUserIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
