package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.OrderDetails;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.repository.OrderRepository;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final MemberIdentityHandlerService memberIdentityHandlerService;

  @Autowired
  OrderService(
      OrderRepository orderRepository,
      ProductRepository productRepository,
      MemberIdentityHandlerService memberIdentityHandlerService) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
    this.memberIdentityHandlerService = memberIdentityHandlerService;
  }

  public ResponseEntity<List<Order>> getAllOrders() {
    try {
      List<Order> orders = orderRepository.findAll();
      if (orders.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(orders);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<Order> getOrderById(String id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order with id " + id + " not found"));

    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    if (currentUser.getRole() != Roles.ADMIN
        && !order.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You are not allowed to view this order");
    }

    return ResponseEntity.ok(order);
  }

  @Transactional
  public ResponseEntity<String> addOrder(OrderRequest orderRequest) {
    User user = memberIdentityHandlerService.getLoggedInUser();

    Order order = new Order();
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user);
    order.setShippingAddress(orderRequest.getShippingAddress());

    BigDecimal orderTotal = BigDecimal.ZERO;
    List<OrderDetails> orderDetails = new ArrayList<>();
    for (OrderRequest.ProductRequest item : orderRequest.getProduct()) {
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(
                  () ->
                      new EntityNotFoundException("Product " + item.getProductId() + " not found"));

      BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
      BigDecimal subTotal = product.getPrice().multiply(quantity);

      OrderDetails orderDetail = new OrderDetails();
      orderDetail.setOrder(order);
      orderDetail.setProduct(product);
      orderDetail.setQuantity(item.getQuantity());
      orderDetail.setUnitPrice(product.getPrice());
      orderDetail.setSubTotal(subTotal);
      orderDetails.add(orderDetail);

      orderTotal = orderTotal.add(subTotal);
    }

    order.setOrderDetails(orderDetails);
    order.setOrderTotal(orderTotal);

    orderRepository.save(order);
    return new ResponseEntity<>("Order Placed Successfully", HttpStatus.OK);
  }

  public ResponseEntity<List<Order>> getOrdersForCustomer() {
    try {
      List<Order> orders =
          orderRepository.getOrderByUserId(memberIdentityHandlerService.getLoggedInMemberID());
      if (orders.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(orders);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
