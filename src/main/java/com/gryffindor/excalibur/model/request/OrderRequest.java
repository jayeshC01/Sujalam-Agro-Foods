package com.gryffindor.excalibur.model.request;

import com.gryffindor.excalibur.model.db.Address;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body payload for creating a customer order")
public class OrderRequest {
  @Valid
  @NotEmpty(message = "Order must contain at least one product")
  @Schema(
      description = "List of products and quantities to order",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private List<ProductRequest> product;

  @Valid
  @NotNull(message = "Shipping address is required")
  @Schema(
      description = "Customer delivery shipping address",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Address shippingAddress;

  @Data
  @Schema(description = "Product line item in an order")
  public static class ProductRequest {
    @NotBlank(message = "Product id cannot be empty")
    @Schema(
        description = "Unique ID of the product being purchased",
        example = "prod_123",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String productId;

    @Min(value = 1, message = "Ordered quantity must be at least 1")
    @Schema(
        description = "Quantity of units to order (min: 1)",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private int orderedQty;
  }
}
