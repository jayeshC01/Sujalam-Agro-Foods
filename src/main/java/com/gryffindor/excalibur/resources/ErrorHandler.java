package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.config.RequestLoggingFilter;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    logger.warn("Access denied: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), null);
  }

  @ExceptionHandler(UserNotRegisteredException.class)
  public ResponseEntity<ErrorResponse> handleUserNotRegisteredException(
      UserNotRegisteredException exception) {
    logger.warn("User not registered: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), null);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException exception) {
    String details =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    logger.warn("Constraint validation failed: {}", details);

    return constructErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Data Constraint validation failed. Please provide correct details",
        details);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException exception) {
    logger.info("Entity not found: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    logger.warn("Data integrity violation", exception);
    return constructErrorResponse(
        HttpStatus.CONFLICT, "A record with the same unique value already exists.", null);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientStockException(
      InsufficientStockException exception) {
    logger.warn("Insufficient stock: {}", exception.getMessage());
    return constructErrorResponse(
        HttpStatus.CONFLICT,
        "This item just went out of stock. Please try again.",
        exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    String details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining("; "));
    logger.warn("Validation failed: {}", details);

    return constructErrorResponse(
        HttpStatus.BAD_REQUEST, "Validation failed. Please provide correct details", details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException exception) {
    logger.warn("Malformed request body", exception);
    return constructErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Malformed request body. Please check the request and try again.",
        null);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException exception) {
    logger.warn("Missing request parameter: {}", exception.getParameterName());
    return constructErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Required request parameter '" + exception.getParameterName() + "' is missing",
        null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException exception) {
    logger.warn("Illegal argument: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleGenericRuntimeError(RuntimeException exception) {
    logger.error("Unhandled runtime exception", exception);
    return constructErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later.",
        null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericError(Exception exception) {
    logger.error("Unhandled exception", exception);
    return constructErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later.",
        null);
  }

  private ResponseEntity<ErrorResponse> constructErrorResponse(
      HttpStatus status, String message, String details) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setCode(status);
    errorResponse.setMessage(message);
    errorResponse.setDetails(details);
    errorResponse.setRequestId(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY));

    return new ResponseEntity<>(errorResponse, status);
  }
}
