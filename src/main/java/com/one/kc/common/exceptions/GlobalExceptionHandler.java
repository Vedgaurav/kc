package com.one.kc.common.exceptions;

import com.one.kc.common.dto.ErrorResponseDto;
import com.one.kc.common.utils.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Exceptions
     * Sends only the message, no stack trace.
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceAlreadyExists(ResourceAlreadyExistsException resourceAlreadyExistsException) {

        // Optional: log internally
        LoggerUtils.error(logger, resourceAlreadyExistsException.toString());

        return new ResponseEntity<>(ErrorResponseDto.builder().status(HttpStatus.CONFLICT.value()).errorMessage(resourceAlreadyExistsException.getMessage()).build(), HttpStatus.CONFLICT);

    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException resourceNotFoundException) {

        // Optional: log internally
        LoggerUtils.error(logger, resourceNotFoundException.toString());

        return new ResponseEntity<>(ErrorResponseDto.builder().status(HttpStatus.NOT_FOUND.value()).errorMessage(resourceNotFoundException.getMessage()).build(), HttpStatus.NOT_FOUND);

    }

    /**
     * Handle validation errors (e.g., @Valid failures)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", "Validation failed");
        response.put("details", errors);


        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "Something went wrong");

        // Optional: log full exception internally
        ex.printStackTrace();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserFacingException.class)
    public ResponseEntity<ErrorResponseDto> handleUserFacingException(UserFacingException userFacingException){
        logger.error(userFacingException.getMessage());
        return new ResponseEntity<>(ErrorResponseDto.builder().status(HttpStatus.UNAUTHORIZED.value()).errorMessage( userFacingException.getMessage()).errorName(HttpStatus.UNAUTHORIZED.name()).build(), HttpStatus.UNAUTHORIZED);
    }
}

