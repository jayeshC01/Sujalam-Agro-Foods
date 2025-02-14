package com.gryffindor.excalibur.services;

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
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.NO_CONTENT);
        errorResponse.setCode(HttpStatus.NO_CONTENT.value());
        errorResponse.setMessage(exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.BAD_REQUEST);
        errorResponse.setCode(HttpStatus.BAD_REQUEST.value());
        errorResponse.setMessage("Data Constraint validation failed. Please provide correct details");
        errorResponse.setDetails(exception.getConstraintViolations().toString());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericRuntimeError(RuntimeException exception) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setMessage(exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setMessage(exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
