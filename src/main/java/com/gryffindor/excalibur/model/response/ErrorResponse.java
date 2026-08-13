package com.gryffindor.excalibur.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ErrorResponse {
  private HttpStatus code;
  private String message;
  private String details;
  private String requestId;
}
