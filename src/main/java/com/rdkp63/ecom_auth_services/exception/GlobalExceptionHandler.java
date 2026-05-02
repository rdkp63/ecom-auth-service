package com.rdkp63.ecom_auth_services.exception;

import com.rdkp63.ecom_auth_services.dto.responseDTO.ErrorResponse;
import com.rdkp63.ecom_auth_services.enums.ErrorDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------- Validation Errors ----------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String field = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    errors.put(field, message);
                });

        return ResponseEntity.badRequest().body(errors);
    }

    // ---------------- Authentication Errors ----------------
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorDetails errorDetails = ex.getErrorDetails();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ErrorResponse.builder()
                                .status(errorDetails.getStatus())
                                .errorCode(errorDetails.getErrorCode())
                                .errorMessage(errorDetails.getErrorMessage())
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        ErrorDetails errorDetails = ex.getErrorDetails();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ErrorResponse.builder()
                                .status(errorDetails.getStatus())
                                .errorCode(errorDetails.getErrorCode())
                                .errorMessage(errorDetails.getErrorMessage())
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    // ---------------- Custom Runtime Errors ----------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {

        // For now treat all runtime exceptions as 400
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse.builder()
                                .status(HttpStatus.BAD_GATEWAY)
                                .errorCode("BAD_REQUEST")
                                .errorMessage(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }

    // ---------------- Fallback / Unknown Errors ----------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        ex.printStackTrace(); // helpful during dev, remove later

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .errorCode("INTERNAL_SERVER_ERROR")
                                .errorMessage("Something went wrong. Please try again.")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
    }
}
