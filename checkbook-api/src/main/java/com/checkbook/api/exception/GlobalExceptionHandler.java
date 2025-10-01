package com.checkbook.api.exception;

import com.checkbook.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for consistent error responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        String message = "Authentication failed";
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
        } else if (ex instanceof DisabledException) {
            message = "Account is disabled";
        } else if (ex instanceof LockedException) {
            message = "Account is locked";
        }

        ErrorResponse error = createErrorResponse(
            status, message, request.getRequestURI(), null
        );

        logger.warn("Authentication failed for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Access denied. You don't have permission to access this resource",
            request.getRequestURI(),
            null
        );

        logger.warn("Access denied for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse error = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            fieldErrors
        );

        logger.warn("Validation failed for request {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            violations.put(propertyPath, message);
        }

        ErrorResponse error = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Constraint violation",
            request.getRequestURI(),
            violations
        );

        logger.warn("Constraint violation for request {}: {}", request.getRequestURI(), violations);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Data integrity violation";

        // Check for common constraint violations
        String rootMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";
        if (rootMessage.contains("unique constraint") || rootMessage.contains("duplicate key")) {
            if (rootMessage.contains("username")) {
                message = "Username already exists";
            } else if (rootMessage.contains("email")) {
                message = "Email address already exists";
            } else if (rootMessage.contains("uk_account_permissions_account_user")) {
                message = "Permission already exists for this user and account";
            } else if (rootMessage.contains("accounts") && rootMessage.contains("name")) {
                message = "Account name already exists";
            } else {
                message = "Duplicate entry detected";
            }
        } else if (rootMessage.contains("foreign key constraint")) {
            if (rootMessage.contains("fk_accounts_owner") || rootMessage.contains("owner_id")) {
                message = "Invalid user reference";
            } else if (rootMessage.contains("fk_account_permissions")) {
                message = "Invalid account or user reference";
            } else {
                message = "Referenced record does not exist";
            }
        }

        ErrorResponse error = createErrorResponse(
            HttpStatus.CONFLICT,
            message,
            request.getRequestURI(),
            null
        );

        logger.warn("Data integrity violation for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request body format",
            request.getRequestURI(),
            null
        );

        logger.warn("Invalid request body for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'",
            ex.getValue(), ex.getName());

        ErrorResponse error = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            message,
            request.getRequestURI(),
            null
        );

        logger.warn("Method argument type mismatch for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("HTTP method '%s' is not supported for this endpoint",
            ex.getMethod());

        ErrorResponse error = createErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED,
            message,
            request.getRequestURI(),
            null
        );

        logger.warn("Method not supported for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Endpoint not found",
            request.getRequestURI(),
            null
        );

        logger.warn("No handler found for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI(),
            null
        );

        logger.warn("Illegal argument for request {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request.getRequestURI(),
            null
        );

        logger.error("Runtime exception for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        ErrorResponse error = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request.getRequestURI(),
            null
        );

        logger.error("Unexpected exception for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ErrorResponse createErrorResponse(HttpStatus status, String message, String path,
                                            Map<String, String> details) {
        ErrorResponse error = new ErrorResponse();
        error.setTimestamp(LocalDateTime.now());
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(message);
        error.setPath(path);

        // Add request ID if available
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            error.setRequestId(requestId);
        }

        if (details != null && !details.isEmpty()) {
            error.setDetails(details);
        }

        return error;
    }
}