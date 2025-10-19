package com.gymcrm.gym_crm_spring.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private ApiErrorResponse buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserExists(UserAlreadyExistsException ex, HttpServletRequest req) {
        var error = buildErrorResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        var error = buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        var error = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(TrainingTypeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTrainingTypeNotFound(
            TrainingTypeNotFoundException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "TRAINING_TYPE_NOT_FOUND",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(TraineeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTraineeNotFound(
            TraineeNotFoundException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "TRAINEE_NOT_FOUND",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {

        StringBuilder message = new StringBuilder("Validation failed: ");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append(String.format("[%s: %s] ", error.getField(), error.getDefaultMessage()));
        }

        var errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message.toString().trim(),
                req.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                ex.getParameterName() + " parameter is required",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TrainerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTrainerNotFound(
            TrainerNotFoundException ex, HttpServletRequest req
    ) {
        var error = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "TRAINER_NOT_FOUND",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest req
    ) {
        String message = String.format("Invalid value for parameter '%s': '%s'. Expected type: %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        var error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER_TYPE",
                message,
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonParseError(
            HttpMessageNotReadableException ex,
            HttpServletRequest req
    ) {
        Throwable cause = ex.getMostSpecificCause();
        String message;

        if (cause instanceof InvalidFormatException ife) {
            if (ife.getCause() instanceof DateTimeParseException) {
                message = "Invalid date format. Use yyyy-MM-dd (e.g. 2025-10-15)";
            } else {
                message = String.format(
                        "Invalid value '%s' for field '%s'. Expected type: %s",
                        ife.getValue(),
                        ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName(),
                        ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "unknown"
                );
            }
        } else if (cause instanceof DateTimeParseException) {
            message = "Invalid date format. Use yyyy-MM-dd (e.g. 2025-10-15)";
        } else {
            message = "Invalid JSON format or value type: " + cause.getMessage();
        }

        var error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                message,
                req.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}