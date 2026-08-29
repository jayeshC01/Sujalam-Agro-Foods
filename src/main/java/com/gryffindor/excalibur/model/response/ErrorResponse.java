package com.gryffindor.excalibur.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(description = "Standardized error response payload")
public class ErrorResponse {
  @Schema(description = "HTTP status code name", example = "BAD_REQUEST")
  private HttpStatus code;

  @Schema(
      description = "Human-readable error explanation",
      example = "Restock quantity must be greater than 0")
  private String message;

  @Schema(description = "Optional additional validation or context details")
  private String details;

  @Schema(
      description = "Unique correlation request ID for troubleshooting and log tracing",
      example = "c8b74681-42e1-45da-9c86-c56ab859ec11")
  private String requestId;
}
