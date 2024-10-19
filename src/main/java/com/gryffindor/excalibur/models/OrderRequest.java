package com.gryffindor.excalibur.models;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {
  // TODO: Remove this later. Fetch this info from session.
  private String customerId;
  private List<ProductRequest> product;
  private long orderTotal;

  @Data
  public static class ProductRequest {
    private String productId;
    private int quantity;
  }
}
