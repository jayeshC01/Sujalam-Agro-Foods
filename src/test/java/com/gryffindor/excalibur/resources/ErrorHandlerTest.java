package com.gryffindor.excalibur.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

class ErrorHandlerTest {

  private final ErrorHandler errorHandler = new ErrorHandler();

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
  void handleIllegalArgumentException_returnsBadRequest() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleIllegalArgumentException(
            new IllegalArgumentException("Invalid order status transition from A to B"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage())
        .isEqualTo("Invalid order status transition from A to B");
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
}
