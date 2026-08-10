package com.gryffindor.excalibur.resources;

import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.services.OrderService;
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
  public ResponseEntity<Order> getOrder(@PathVariable String id) {
    return orderService.getOrderById(id);
  }

  @GetMapping("/orders")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Order>> getOrders() {
    return orderService.getAllOrders();
  }

  @GetMapping("/customer/orders")
  public ResponseEntity<List<Order>> getCustomerOrders() {
    return orderService.getOrdersForCustomer();
  }

  @PostMapping("/orders")
  public ResponseEntity<String> createOrder(@RequestBody OrderRequest order) {
    return orderService.addOrder(order);
  }
}
