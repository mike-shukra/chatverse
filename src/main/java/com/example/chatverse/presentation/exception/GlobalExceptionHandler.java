package com.example.chatverse.presentation.exception;

import com.example.chatverse.application.dto.response.ErrorResponse;
import org.apache.kafka.common.errors.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException; // Для ошибок конвертации типов параметров

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors; // Для MethodArgumentNotValidException

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class); // Добавляем логгер

    // Обработчик для ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getDescription(false));
    }

    // Обработчик для DuplicateResourceException
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getDescription(false)); // 409 Conflict
    }

    // Обработчик для InvalidCredentialsException
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex, WebRequest request) {
        log.warn("Invalid credentials: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getDescription(false)); // 401 Unauthorized
    }

    // Обработчик для кастомных исключений аутентификации (если нужны)
    @ExceptionHandler(UserNotAuthenticatedException.class) // Пример из твоего UserController
    public ResponseEntity<ErrorResponse> handleUserNotAuthenticatedException(
            UserNotAuthenticatedException ex, WebRequest request) {
        log.warn("User not authenticated: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getDescription(false));
    }

    @ExceptionHandler(InvalidTokenUserIdException.class) // Пример из твоего UserController
    public ResponseEntity<ErrorResponse> handleInvalidTokenUserIdException(
            InvalidTokenUserIdException ex, WebRequest request) {
        log.warn("Invalid user ID in token: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false));
    }


    // Существующий обработчик для AccessDeniedException (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Access is denied";
        log.warn("Access denied: {} (Path: {})", errorMessage, request.getDescription(false));
        return buildErrorResponse(HttpStatus.FORBIDDEN, errorMessage, request.getDescription(false));
    }

    // Улучшенный обработчик для MethodArgumentNotValidException (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        // Собираем все сообщения об ошибках валидации
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (errorMessage.isEmpty() && ex.getBindingResult().getGlobalError() != null) {
            errorMessage = ex.getBindingResult().getGlobalError().getDefaultMessage();
        }
        log.warn("Validation error: {} (Path: {})", errorMessage, request.getDescription(false));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage, request.getDescription(false));
    }

    // Обработчик для ошибок конвертации типов параметров пути/запроса
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String errorMessage = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("Method argument type mismatch: {} (Path: {})", errorMessage, request.getDescription(false));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMessage, request.getDescription(false));
    }


    // Обработчик для IllegalArgumentException (можно оставить для общих случаев или сделать более специфичным)
    // Если большинство случаев покрыты кастомными исключениями, этот может стать реже использоваться
    // или использоваться для действительно "непредвиденных" неверных аргументов на уровне логики.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {} (Path: {})", ex.getMessage(), request.getDescription(false));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getDescription(false));
    }

    // "Catch-all" обработчик для всех остальных исключений (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        // Важно логировать полный стектрейс для неизвестных ошибок
        log.error("An unexpected error occurred (Path: {})", request.getDescription(false), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred. Please try again later.", request.getDescription(false));
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        // Удаляем query string из path, если он есть, для большей чистоты
        String cleanPath = path.startsWith("uri=") ? path.substring(4) : path;
        int queryParamStartIndex = cleanPath.indexOf('?');
        if (queryParamStartIndex != -1) {
            cleanPath = cleanPath.substring(0, queryParamStartIndex);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(cleanPath)
                .build();
        return new ResponseEntity<>(errorResponse, status);
    }
}