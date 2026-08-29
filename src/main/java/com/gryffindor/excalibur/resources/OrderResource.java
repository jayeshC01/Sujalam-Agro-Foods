package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.request.UpdateOrderStatusRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@Tag(
    name = "Orders",
    description =
        "Customer order placement, status tracking, cancellation, and admin order fulfillment")
public class OrderResource {
  private final OrderService orderService;

  @Autowired
  OrderResource(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/order/{id}")
  @Operation(
      summary = "Get order by ID",
      description =
          "Fetches details of an order. Accessible by the order owner or an administrator.")
  public ResponseEntity<OrderResponse> getOrder(
      @Parameter(description = "Order unique identifier", example = "ord_abc123") @PathVariable
          String id) {
    return orderService.getOrderById(id);
  }

  @GetMapping("/admin/orders")
  @Operation(
      summary = "Admin: List all customer orders",
      description =
          "Paginated order list for administrators with optional status, customer ID, and creation date filters")
  public ResponseEntity<PageResponse<OrderResponse>> getOrders(
      @Parameter(description = "Filter by order status (PENDING, COMPLETED, CANCELED)")
          @RequestParam(required = false)
          OrderStatus status,
      @Parameter(description = "Filter by customer ID", example = "usr_456")
          @RequestParam(required = false)
          String customerId,
      @Parameter(description = "Alternative parameter for customer ID")
          @RequestParam(required = false, name = "customerID")
          String customerIdAlt,
      @Parameter(description = "Filter by order creation date (YYYY-MM-DD)", example = "2026-08-29")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate createdAt,
      @Parameter(description = "Page number (0-based index)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of items per page (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(1)
          @Max(100)
          int size,
      @Parameter(description = "Field to sort by (createdAt, updatedAt)", example = "createdAt")
          @RequestParam(defaultValue = "createdAt")
          String sortBy,
      @Parameter(description = "Sort direction (asc, desc)", example = "desc")
          @RequestParam(defaultValue = "desc")
          String sort) {
    String resolvedCustomerId = customerId != null ? customerId : customerIdAlt;
    return orderService.getAllOrders(
        status, resolvedCustomerId, createdAt, page, size, sortBy, sort);
  }

  @GetMapping("/orders")
  @Operation(
      summary = "List current customer's orders",
      description = "Fetches a paginated history of orders placed by the authenticated customer")
  public ResponseEntity<PageResponse<OrderResponse>> getCustomerOrders(
      @Parameter(description = "Page number (0-based index)", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of items per page (1-100)", example = "10")
          @RequestParam(defaultValue = "10")
          @Min(1)
          @Max(100)
          int size) {
    return orderService.getOrdersForCustomer(page, size);
  }

  @PostMapping("/orders")
  @Operation(
      summary = "Place a new order",
      description =
          "Creates a new order, validates & reserves product stock, and triggers confirmation emails. Protected with idempotency key.")
  public ResponseEntity<OrderResponse> createOrder(
      @Parameter(
              description =
                  "Unique client-generated idempotency key (UUID) to prevent double-charging",
              example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
              required = true)
          @RequestHeader(value = "Idempotency-Key", required = true)
          String idempotencyKey,
      @Valid @RequestBody OrderRequest order) {
    return orderService.addOrder(order, idempotencyKey);
  }

  @PostMapping("/orders/{id}/cancel")
  @Operation(
      summary = "Cancel an order",
      description =
          "Cancels a PENDING order and automatically restores reserved inventory units to stock")
  public ResponseEntity<OrderResponse> cancelOrder(
      @Parameter(description = "Order unique identifier", example = "ord_abc123") @PathVariable
          String id) {
    return orderService.cancelOrder(id);
  }

  @PatchMapping("/admin/orders/{id}/status")
  @Operation(
      summary = "Admin: Update order status",
      description = "Updates status of an order (e.g. mark as COMPLETED or CANCELED)")
  public ResponseEntity<OrderResponse> updateOrderStatus(
      @Parameter(description = "Order unique identifier", example = "ord_abc123") @PathVariable
          String id,
      @Valid @RequestBody UpdateOrderStatusRequest request) {
    return orderService.updateOrderStatus(id, request.getStatus());
  }
}
