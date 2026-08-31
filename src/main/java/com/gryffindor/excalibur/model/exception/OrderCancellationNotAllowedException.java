package com.gryffindor.excalibur.model.exception;

public class OrderCancellationNotAllowedException extends RuntimeException {
  public OrderCancellationNotAllowedException(String message) {
    super(message);
  }
}
