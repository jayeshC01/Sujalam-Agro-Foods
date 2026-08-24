package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
  private String id;
  private OrderStatus orderStatus;
  private CustomerResponse customer;
  private BigDecimal subTotal;
  private BigDecimal taxAmount;
  private BigDecimal deliveryCharge;
  private BigDecimal grandTotal;
  private Address shippingAddress;
  private List<OrderDetailsResponse> orderDetails;

  public static OrderResponse from(Order order) {
    return OrderResponse.builder()
        .id(order.getOrderId())
        .orderStatus(order.getOrderStatus())
        .customer(CustomerResponse.from(order.getUser()))
        .subTotal(order.getSubTotal())
        .taxAmount(order.getTaxAmount())
        .deliveryCharge(order.getDeliveryCharge())
        .grandTotal(order.getGrandTotal())
        .shippingAddress(order.getShippingAddress())
        .orderDetails(
            order.getOrderDetails() == null
                ? List.of()
                : order.getOrderDetails().stream().map(OrderDetailsResponse::from).toList())
        .build();
  }
}
