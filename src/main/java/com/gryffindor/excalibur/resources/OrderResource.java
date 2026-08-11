package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.request.UpdateOrderStatusRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.services.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
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
  public ResponseEntity<List<OrderResponse>> getOrders() {
    return orderService.getAllOrders();
  }

  @GetMapping("/customer/orders")
  public ResponseEntity<List<OrderResponse>> getCustomerOrders() {
    return orderService.getOrdersForCustomer();
  }

  @PostMapping("/orders")
  public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest order) {
    return orderService.addOrder(order);
  }

  @PatchMapping("/admin/orders/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<OrderResponse> updateOrderStatus(
      @PathVariable String id, @Valid @RequestBody UpdateOrderStatusRequest request) {
    return orderService.updateOrderStatus(id, request.getStatus());
  }
}
