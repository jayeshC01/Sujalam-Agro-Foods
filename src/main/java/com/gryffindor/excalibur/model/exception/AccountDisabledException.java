package com.gryffindor.excalibur.model.exception;

public class AccountDisabledException extends RuntimeException {
  public AccountDisabledException(String message) {
    super(message);
  }
}
