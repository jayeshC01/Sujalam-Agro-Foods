package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.db.Customer;
import com.gryffindor.excalibur.db.Order;
import com.gryffindor.excalibur.db.OrderDetails;
import com.gryffindor.excalibur.db.Product;
import com.gryffindor.excalibur.models.OrderRequest;
import com.gryffindor.excalibur.repository.CustomerRepository;
import com.gryffindor.excalibur.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OrderService {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private CustomerRepository customerRepository;

  OrderService() {}

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

  public ResponseEntity<String> addOrder(String customerId, OrderRequest orderRequest) {
    try {
      Order order = new Order();
      order.setDate(new Date());
      order.setOrderStatus(Order.OrderStatus.PENDING);
      Customer customer = customerRepository.findById(customerId).orElse(null);
      if(customer != null) {
        order.setCustomer(customer);
      }
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
                    return orderDetail;
                  }).toList();
      order.setOrderDetails(orderDetails);

      orderRepository.save(order);
      return new ResponseEntity<>("Order Placed Successfully", HttpStatus.OK);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public ResponseEntity<List<Order>> getOrdersForCustomer(String customerId) {
    try {
      List<Order> orders = orderRepository.getOrderByCustomerId(customerId);
      if(orders.isEmpty()) {
        return ResponseEntity.noContent().build();
      }
      return ResponseEntity.ok(orders);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
