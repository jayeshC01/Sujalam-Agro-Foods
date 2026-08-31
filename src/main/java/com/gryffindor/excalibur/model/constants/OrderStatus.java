package com.gryffindor.excalibur.model.constants;

public enum OrderStatus {
  PROCESSING,
  PACKED,
  SHIPPED,
  COMPLETED,
  CANCELED;

  public boolean canTransitionTo(OrderStatus targetStatus) {
    return switch (this) {
      case PROCESSING -> targetStatus == PACKED || targetStatus == CANCELED;
      case PACKED -> targetStatus == SHIPPED || targetStatus == CANCELED;
      case SHIPPED -> targetStatus == COMPLETED || targetStatus == CANCELED;
      case COMPLETED, CANCELED -> false;
    };
  }

  public boolean isCancellableByCustomer() {
    return this == PROCESSING;
  }
}
