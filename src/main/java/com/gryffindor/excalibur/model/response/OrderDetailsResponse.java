package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.db.OrderDetails;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailsResponse {
  private String id;
  private String productId;
  private String productName;
  private Integer orderedQty;
  private BigDecimal unitPrice;
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
