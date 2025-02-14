package com.gryffindor.excalibur.controllers;

import com.gryffindor.excalibur.models.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(HttpStatus.NOT_FOUND)
                .message(exception.getMessage())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericRuntimeError(RuntimeException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(exception.getMessage())
                .stackTrace(null)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(exception.getMessage())
                .stackTrace(null)
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST)
                .message(exception.getMessage())
                .stackTrace(exception.getConstraintViolations().toString())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
