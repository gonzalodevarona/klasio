package com.klasio.shared.infrastructure.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        var error = new ErrorResponse.ErrorDetail("MAX_UPLOAD_SIZE_EXCEEDED", "File size exceeds the maximum allowed limit");
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        var error = new ErrorResponse.ErrorDetail("ACCESS_DENIED", "Insufficient permissions");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(error));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        var error = new ErrorResponse.ErrorDetail("TENANT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(error));
    }

    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSlugAlreadyExists(SlugAlreadyExistsException ex) {
        var error = new ErrorResponse.ErrorDetail("SLUG_ALREADY_EXISTS", ex.getMessage(), null, ex.getSuggestedSlug());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(TenantAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantAlreadyInactive(TenantAlreadyInactiveException ex) {
        var error = new ErrorResponse.ErrorDetail("TENANT_ALREADY_INACTIVE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(error));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileType(InvalidFileTypeException ex) {
        var error = new ErrorResponse.ErrorDetail("INVALID_FILE_TYPE", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR",
                "Required parameter '%s' is missing".formatted(ex.getParameterName()));
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        var error = new ErrorResponse.ErrorDetail("VALIDATION_ERROR", ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        var error = new ErrorResponse.ErrorDetail("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(error));
    }
}
