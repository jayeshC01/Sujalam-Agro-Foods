package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.OrderDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Line item detail for an ordered product")
public class OrderDetailsResponse {
  @Schema(
      description = "Line item identifier (UUID)",
      example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(
      description = "Product unique identifier",
      example = "550e8400-e29b-41d4-a716-446655440000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String productId;

  @Schema(
      description = "Product name at time of order",
      example = "Cold-Pressed Groundnut Oil (1L)",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String productName;

  @Schema(
      description = "Quantity purchased",
      example = "2",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer orderedQty;

  @Schema(
      description = "Unit price in INR at time of purchase",
      example = "240.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal unitPrice;

  @Schema(
      description = "Line item subtotal (orderedQty * unitPrice) in INR",
      example = "480.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal subTotal;

  public static OrderDetailsResponse from(OrderDetails orderDetails) {
    return OrderDetailsResponse.builder()
        .id(orderDetails.getId())
        .productId(orderDetails.getProduct().getId())
        .productName(orderDetails.getProduct().getName())
        .orderedQty(orderDetails.getOrderedQty())
        .unitPrice(orderDetails.getUnitPrice())
        .subTotal(orderDetails.getSubTotal())
        .build();
  }
}
