package com.gryffindor.excalibur.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gryffindor.excalibur.model.exception.AccountDisabledException;
import com.gryffindor.excalibur.model.exception.AuthenticationProviderException;
import com.gryffindor.excalibur.model.exception.DuplicateProductException;
import com.gryffindor.excalibur.model.exception.EmailNotVerifiedException;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.exception.InvalidOrderStatusTransitionException;
import com.gryffindor.excalibur.model.exception.InvalidRequestException;
import com.gryffindor.excalibur.model.exception.OrderCancellationNotAllowedException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class ErrorHandlerTest {

  private final ErrorHandler errorHandler = new ErrorHandler();

  @Test
  void handleAccountDisabledException_returnsForbidden() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAccountDisabledException(
            new AccountDisabledException("Your account has been deactivated."));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Your account has been deactivated.");
  }

  @Test
  void handleAuthenticationProviderException_returnsInternalServerError() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAuthenticationProviderException(
            new AuthenticationProviderException("Failed to reach auth provider"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Failed to update authentication provider. Please try again later.");
  }

  @Test
  void handleEntityNotFoundException_returnsNotFound() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleEntityNotFoundException(
            new EntityNotFoundException("Product with id 1 not found"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Product with id 1 not found");
  }

  @Test
  void handleUserNotRegisteredException_returnsForbiddenWithClearMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleUserNotRegisteredException(
            new UserNotRegisteredException(
                "No profile found for this account. Please complete registration first."));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("No profile found for this account. Please complete registration first.");
  }

  @Test
  void handleAuthenticationException_returnsUnauthorizedWithCustomMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAuthenticationException(
            new BadCredentialsException("Invalid token signature"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Invalid token signature");
  }

  @Test
  void handleAuthenticationException_returnsUnauthorizedWithDefaultMessage_whenBlank() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAuthenticationException(new InsufficientAuthenticationException(""));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Authentication required. Please provide a valid Bearer token.");
  }

  @Test
  void handleAccessDeniedException_returnsForbiddenWithCustomMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAccessDeniedException(
            new AccessDeniedException("Admins cannot delete their own account"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Admins cannot delete their own account");
  }

  @Test
  void handleAccessDeniedException_returnsForbiddenWithDefaultMessage_whenBlank() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleAccessDeniedException(new AccessDeniedException(""));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Access denied. You do not have permission to access this resource.");
  }

  @Test
  void handleInvalidOrderStatusTransitionException_returnsBadRequest() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleInvalidOrderStatusTransitionException(
            new InvalidOrderStatusTransitionException(
                "Invalid order status transition from A to B"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Invalid order status transition from A to B");
  }

  @Test
  void handleInvalidRequestException_returnsBadRequest() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleInvalidRequestException(
            new InvalidRequestException("Invalid sort direction: 'sideways'"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Invalid sort direction: 'sideways'");
  }

  @Test
  void handleConstraintViolationException_returnsBadRequest() {
    ConstraintViolationException exception =
        new ConstraintViolationException("invalid", Collections.emptySet());

    ResponseEntity<ErrorResponse> response =
        errorHandler.handleConstraintViolationException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Data Constraint validation failed. Please provide correct details");
    assertThat(response.getBody().getDetails()).isNotNull();
  }

  @Test
  void handleDuplicateProductException_returnsConflictWithMessage() {
    DuplicateProductException exception =
        new DuplicateProductException("Product with name 'Rice' already exists");

    ResponseEntity<ErrorResponse> response =
        errorHandler.handleDuplicateProductException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Product with name 'Rice' already exists");
  }

  @Test
  void handleDataIntegrityViolationException_returnsConflict() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException(
                "Duplicate entry 'john@example.com' for key 'users.UK_email'"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("A record with the same unique value already exists.");
  }

  @Test
  void handleDataAccessException_returnsServiceUnavailableWithoutLeakingRawMessage() {
    DataAccessException exception =
        new DataAccessResourceFailureException(
            "Communications link failure: jdbc:mysql://admin:secret@10.0.0.1:3306/prod");

    ResponseEntity<ErrorResponse> response = errorHandler.handleDataAccessException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Database service is temporarily unavailable. Please try again later.");
  }

  @Test
  void handleInsufficientStockException_returnsConflictWithFrontendFriendlyMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleInsufficientStockException(
            new InsufficientStockException(
                "Insufficient stock for product 'Almonds'. Available: 1, requested: 2"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("This item just went out of stock. Please try again.");
    assertThat(response.getBody().getDetails())
        .isEqualTo("Insufficient stock for product 'Almonds'. Available: 1, requested: 2");
  }

  @Test
  void handleGenericRuntimeError_returnsInternalServerErrorWithoutLeakingRawMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleGenericRuntimeError(
            new RuntimeException("boom: password=hunter2 at com.gryffindor.excalibur.Internal"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("An unexpected error occurred. Please try again later.");
  }

  @Test
  void handleGenericError_returnsInternalServerErrorWithoutLeakingRawMessage() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleGenericError(
            new Exception("unexpected: password=hunter2 at com.gryffindor.excalibur.Internal"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("An unexpected error occurred. Please try again later.");
  }

  @Test
  void handleHttpMessageNotReadableException_returnsBadRequestWithoutLeakingRawMessage() {
    HttpMessageNotReadableException exception =
        new HttpMessageNotReadableException(
            "JSON parse error: Cannot deserialize value of type `Category` from String \"BAD\"",
            mock(HttpInputMessage.class));

    ResponseEntity<ErrorResponse> response =
        errorHandler.handleHttpMessageNotReadableException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Malformed request body. Please check the request and try again.");
  }

  @Test
  void handleEmailNotVerifiedException_returnsForbidden() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleEmailNotVerifiedException(
            new EmailNotVerifiedException(
                "Email is not verified. Please verify your email address and log in again."));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Email is not verified. Please verify your email address and log in again.");
  }

  @Test
  void handleOrderCancellationNotAllowedException_returnsBadRequest() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleOrderCancellationNotAllowedException(
            new OrderCancellationNotAllowedException(
                "Order cannot be cancelled once it has been packed or shipped. Current status: PACKED"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo(
            "Order cannot be cancelled once it has been packed or shipped. Current status: PACKED");
  }
}
