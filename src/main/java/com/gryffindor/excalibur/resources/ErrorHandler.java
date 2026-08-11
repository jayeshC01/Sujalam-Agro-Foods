package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.FORBIDDEN);
    errorResponse.setMessage(exception.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exception) {
    String details =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.BAD_REQUEST);
    errorResponse.setMessage("Data Constraint validation failed. Please provide correct details");
    errorResponse.setDetails(details);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException exception) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.NOT_FOUND);
    errorResponse.setMessage(exception.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    logger.warn("Data integrity violation", exception);

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.CONFLICT);
    errorResponse.setMessage("A record with the same unique value already exists.");

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientStockException(
      InsufficientStockException exception) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.CONFLICT);
    errorResponse.setMessage("This item just went out of stock. Please try again.");
    errorResponse.setDetails(exception.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    String details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.BAD_REQUEST);
    errorResponse.setMessage("Validation failed. Please provide correct details");
    errorResponse.setDetails(details);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception) {
    logger.warn("Malformed request body", exception);

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.BAD_REQUEST);
    errorResponse.setMessage("Malformed request body. Please check the request and try again.");

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleGenericRuntimeError(RuntimeException exception) {
    logger.error("Unhandled runtime exception", exception);

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR);
    errorResponse.setMessage("An unexpected error occurred. Please try again later.");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
    logger.error("Unhandled exception", exception);

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR);
    errorResponse.setMessage("An unexpected error occurred. Please try again later.");

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
