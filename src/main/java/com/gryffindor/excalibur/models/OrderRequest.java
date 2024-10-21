package com.gryffindor.excalibur.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
  private List<ProductRequest> product;
  private long orderTotal;

  @Data
  public static class ProductRequest {
    private String productId;
    private int quantity;
  }
}
