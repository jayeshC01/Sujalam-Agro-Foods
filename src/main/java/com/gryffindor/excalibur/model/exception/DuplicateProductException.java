package com.gryffindor.excalibur.model.exception;

public class DuplicateProductException extends RuntimeException {
  public DuplicateProductException(String message) {
    super(message);
  }
}
