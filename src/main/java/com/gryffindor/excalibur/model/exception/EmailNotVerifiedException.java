package com.gryffindor.excalibur.model.exception;

public class EmailNotVerifiedException extends RuntimeException {
  public EmailNotVerifiedException(String message) {
    super(message);
  }
}
