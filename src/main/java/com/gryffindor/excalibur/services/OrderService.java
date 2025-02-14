package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.constants.OrderStatus;
import com.gryffindor.excalibur.models.db.Order;
import com.gryffindor.excalibur.models.db.OrderDetails;
import com.gryffindor.excalibur.models.db.Product;
import com.gryffindor.excalibur.models.OrderRequest;
import com.gryffindor.excalibur.repository.UserRepository;
import com.gryffindor.excalibur.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final MemberIdentityHandlerService memberIdentityHandlerService;

  @Autowired
  OrderService(OrderRepository orderRepository, UserRepository userRepository, MemberIdentityHandlerService memberIdentityHandlerService) {
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
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
    try {
      Order order = orderRepository.findById(id).orElse(null);
      if (order == null) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok(order);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<String> addOrder(OrderRequest orderRequest) {
    try {
      Order order = new Order();
      order.setDate(new Date());
      order.setOrderStatus(OrderStatus.PENDING);
      userRepository.findById(memberIdentityHandlerService.getLoggedInMemberID()).ifPresent(order::setUser);
      order.setOrderTotal(orderRequest.getOrderTotal());
      List<OrderDetails> orderDetails =
          orderRequest.getProduct()
              .stream()
                  .map(orderItem -> {
                    OrderDetails orderDetail = new OrderDetails();
                    Product product = new Product();
                    product.setId(orderItem.getProductId());
                    orderDetail.setQuantity(orderItem.getQuantity());
                    orderDetail.setProduct(product);
                    orderDetail.setTotal(500L);
                    return orderDetail;
                  }).toList();
      order.setOrderDetails(orderDetails);

      orderRepository.save(order);
      return new ResponseEntity<>("Order Placed Successfully", HttpStatus.OK);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public ResponseEntity<List<Order>> getOrdersForCustomer() {
    try {
      List<Order> orders = orderRepository.getOrderByUserId(memberIdentityHandlerService.getLoggedInMemberID());
      if(orders.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(orders);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
