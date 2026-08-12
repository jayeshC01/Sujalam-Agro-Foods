package com.gryffindor.excalibur.model.constants;

public enum OrderStatus {
  PENDING,
  COMPLETED,
  CANCELED;

  public boolean canTransitionTo(OrderStatus targetStatus) {
    return switch (this) {
      case PENDING -> targetStatus == COMPLETED || targetStatus == CANCELED;
      case COMPLETED, CANCELED -> false;
    };
  }
}
