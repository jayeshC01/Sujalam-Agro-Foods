package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.db.Order;
import com.gryffindor.excalibur.models.OrderRequest;
import com.gryffindor.excalibur.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderResource {
  @Autowired
  private OrderService orderService;

  @GetMapping("/admin/order/{id}")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Order> getOrder(@PathVariable String id) {
    return orderService.getOrderById(id);
  }

  @GetMapping("/admin/orders")
  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<List<Order>> getOrders() {
    return orderService.getAllOrders();
  }

  @GetMapping("/user/orders/customer/{id}")
  @PreAuthorize("hasAuthority('USER')")
  public ResponseEntity<List<Order>> getCustomerOrder(@PathVariable String id) {
    return orderService.getOrdersForCustomer(id);
  }

  @PostMapping("/orders")
  @PreAuthorize("hasAuthority('ADMIN') and hasAuthority('USER')")
  public ResponseEntity<String> createOrder(@RequestParam String customerId,@RequestBody OrderRequest order) {
    return orderService.addOrder(customerId, order);
  }
}
