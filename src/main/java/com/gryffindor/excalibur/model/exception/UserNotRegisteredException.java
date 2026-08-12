package com.gryffindor.excalibur.model.exception;

public class UserNotRegisteredException extends RuntimeException {
  public UserNotRegisteredException(String message) {
    super(message);
  }
}
