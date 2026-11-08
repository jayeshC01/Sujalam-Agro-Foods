package com.gryffindor.excalibur.model.exception;

public class ProductDeletionNotAllowedException extends RuntimeException {
  public ProductDeletionNotAllowedException(String message) {
    super(message);
  }
}
