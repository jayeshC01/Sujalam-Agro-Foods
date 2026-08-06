package com.gryffindor.excalibur.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.gryffindor.excalibur.models.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
  void handleGenericRuntimeError_returnsInternalServerError() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleGenericRuntimeError(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("boom");
  }

  @Test
  void handleGenericError_returnsInternalServerError() {
    ResponseEntity<ErrorResponse> response =
        errorHandler.handleGenericError(new Exception("unexpected"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("unexpected");
  }
}
