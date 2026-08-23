package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.request.UpdateOrderStatusRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class OrderResource {
  private final OrderService orderService;

  @Autowired
  OrderResource(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping("/order/{id}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
    return orderService.getOrderById(id);
  }

  @GetMapping("/orders")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PageResponse<OrderResponse>> getOrders(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    return orderService.getAllOrders(page, size);
  }

  @GetMapping("/customer/orders")
  public ResponseEntity<List<OrderResponse>> getCustomerOrders() {
    return orderService.getOrdersForCustomer();
  }

  @PostMapping("/orders")
  public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest order) {
    return orderService.addOrder(order);
  }

  @PostMapping("/orders/{id}/cancel")
  public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String id) {
    return orderService.cancelOrder(id);
  }

  @PatchMapping("/admin/orders/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrderResponse> updateOrderStatus(
      @PathVariable String id, @Valid @RequestBody UpdateOrderStatusRequest request) {
    return orderService.updateOrderStatus(id, request.getStatus());
  }
}
