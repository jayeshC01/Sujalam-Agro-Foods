package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.db.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
  @Valid
  @NotEmpty(message = "Order must contain at least one product")
  private List<ProductRequest> product;

  @Valid
  @NotNull(message = "Shipping address is required")
  private Address shippingAddress;

  @Data
  public static class ProductRequest {
    @NotBlank(message = "Product id cannot be empty")
    private String productId;

    @Min(value = 1, message = "Ordered quantity must be at least 1")
    private int orderedQty;
  }
}
