package com.gryffindor.excalibur.model.exception;

public class IdempotencyPayloadMismatchException extends RuntimeException {
  public IdempotencyPayloadMismatchException(String message) {
    super(message);
  }
}
