package com.gryffindor.excalibur.model.exception;

public class AuthenticationProviderException extends RuntimeException {

  public AuthenticationProviderException(String message) {
    super(message);
  }

  public AuthenticationProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
