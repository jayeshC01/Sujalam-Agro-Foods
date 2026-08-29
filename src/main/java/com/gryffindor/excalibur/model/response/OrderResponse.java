package com.gryffindor.excalibur.model.response;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Order representation with totals, shipping address, and line items")
public class OrderResponse {
  @Schema(
      description = "Unique order identifier (UUID)",
      example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(
      description = "Current lifecycle status of the order",
      example = "PENDING",
      allowableValues = {"PENDING", "COMPLETED", "CANCELED"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private OrderStatus orderStatus;

  @Schema(
      description = "Customer profile associated with this order",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private CustomerResponse customer;

  @Schema(
      description = "Subtotal before taxes and shipping in INR",
      example = "480.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal subTotal;

  @Schema(
      description = "Calculated GST tax amount in INR",
      example = "24.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal taxAmount;

  @Schema(
      description = "Delivery / shipping charge in INR",
      example = "50.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal deliveryCharge;

  @Schema(
      description = "Final grand total payable in INR",
      example = "554.00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private BigDecimal grandTotal;

  @Schema(description = "Delivery destination address", requiredMode = Schema.RequiredMode.REQUIRED)
  private Address shippingAddress;

  @Schema(
      description = "List of ordered products and itemized prices",
      requiredMode = Schema.RequiredMode.REQUIRED)
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
