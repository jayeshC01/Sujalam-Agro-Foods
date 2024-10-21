package com.gryffindor.excalibur.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
