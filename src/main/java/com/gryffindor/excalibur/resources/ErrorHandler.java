package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.config.RequestLoggingFilter;
import com.gryffindor.excalibur.model.exception.AccountDisabledException;
import com.gryffindor.excalibur.model.exception.AuthenticationProviderException;
import com.gryffindor.excalibur.model.exception.DuplicateProductException;
import com.gryffindor.excalibur.model.exception.EmailNotVerifiedException;
import com.gryffindor.excalibur.model.exception.IdempotencyPayloadMismatchException;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.exception.InvalidOrderStatusTransitionException;
import com.gryffindor.excalibur.model.exception.InvalidRequestException;
import com.gryffindor.excalibur.model.exception.OrderCancellationNotAllowedException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException exception) {
    logger.warn("Authentication failed: {}", exception.getMessage());
    String message =
        (exception.getMessage() != null && !exception.getMessage().isBlank())
            ? exception.getMessage()
            : "Authentication required. Please provide a valid Bearer token.";
    return constructErrorResponse(HttpStatus.UNAUTHORIZED, message, null);
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(
      EmailNotVerifiedException exception) {
    logger.warn("Unverified email operation rejected: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), null);
  }

  @ExceptionHandler(OrderCancellationNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleOrderCancellationNotAllowedException(
      OrderCancellationNotAllowedException exception) {
    logger.warn("Order cancellation rejected: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException exception) {
    logger.warn("Access denied: {}", exception.getMessage());
    String message =
        (exception.getMessage() != null && !exception.getMessage().isBlank())
            ? exception.getMessage()
            : "Access denied. You do not have permission to access this resource.";
    return constructErrorResponse(HttpStatus.FORBIDDEN, message, null);
  }

  @ExceptionHandler(UserNotRegisteredException.class)
  public ResponseEntity<ErrorResponse> handleUserNotRegisteredException(
      UserNotRegisteredException exception) {
    logger.warn("User not registered: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), null);
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ErrorResponse> handleAccountDisabledException(
      AccountDisabledException exception) {
    logger.warn("Deactivated account access attempt: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.FORBIDDEN, exception.getMessage(), null);
  }

  @ExceptionHandler(AuthenticationProviderException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationProviderException(
      AuthenticationProviderException exception) {
    logger.error("Authentication provider operation failed: {}", exception.getMessage(), exception);
    return constructErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to update authentication provider. Please try again later.",
        null);
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

  @ExceptionHandler(DuplicateProductException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateProductException(
      DuplicateProductException exception) {
    logger.warn("Duplicate product: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.CONFLICT, exception.getMessage(), null);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    logger.warn("Data integrity violation", exception);
    return constructErrorResponse(
        HttpStatus.CONFLICT, "A record with the same unique value already exists.", null);
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException exception) {
    logger.error("Database access or connection error", exception);
    return constructErrorResponse(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Database service is temporarily unavailable. Please try again later.",
        null);
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

  @ExceptionHandler(IdempotencyPayloadMismatchException.class)
  public ResponseEntity<ErrorResponse> handleIdempotencyPayloadMismatchException(
      IdempotencyPayloadMismatchException exception) {
    logger.warn("Idempotency payload mismatch: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), null);
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

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
      MissingRequestHeaderException exception) {
    logger.warn("Missing request header: {}", exception.getHeaderName());
    return constructErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Required request header '" + exception.getHeaderName() + "' is missing",
        null);
  }

  @ExceptionHandler(InvalidOrderStatusTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidOrderStatusTransitionException(
      InvalidOrderStatusTransitionException exception) {
    logger.warn("Invalid order status transition: {}", exception.getMessage());
    return constructErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequestException(
      InvalidRequestException exception) {
    logger.warn("Invalid request: {}", exception.getMessage());
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
