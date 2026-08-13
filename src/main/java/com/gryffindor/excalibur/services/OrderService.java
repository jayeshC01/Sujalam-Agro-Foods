package com.gryffindor.excalibur.services;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.OrderDetails;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.repository.OrderRepository;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final MemberIdentityHandlerService memberIdentityHandlerService;
  private final EmailService emailService;

  @Autowired
  OrderService(
      OrderRepository orderRepository,
      ProductRepository productRepository,
      MemberIdentityHandlerService memberIdentityHandlerService,
      EmailService emailService) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
    this.memberIdentityHandlerService = memberIdentityHandlerService;
    this.emailService = emailService;
  }

  public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    Page<Order> orders = orderRepository.findAll(pageRequest);

    List<OrderResponse> content = orders.getContent().stream().map(OrderResponse::from).toList();
    PageResponse<OrderResponse> pageResponse =
        PageResponse.<OrderResponse>builder()
            .content(content)
            .page(orders.getNumber())
            .size(orders.getSize())
            .totalElements(orders.getTotalElements())
            .totalPages(orders.getTotalPages())
            .first(orders.isFirst())
            .last(orders.isLast())
            .build();

    return ResponseEntity.ok(pageResponse);
  }

  public ResponseEntity<OrderResponse> getOrderById(String id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order with id " + id + " not found"));

    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    if (currentUser.getRole() != Roles.ADMIN
        && !order.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You are not allowed to view this order");
    }

    return ResponseEntity.ok(OrderResponse.from(order));
  }

  @Transactional
  public ResponseEntity<OrderResponse> addOrder(OrderRequest orderRequest) {
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

      int updatedRows = productRepository.decrementStock(item.getProductId(), item.getOrderedQty());
      if (updatedRows == 0) {
        throw new InsufficientStockException(
            "Insufficient stock for product '"
                + product.getName()
                + "'. Available: "
                + product.getQty()
                + ", requested: "
                + item.getOrderedQty());
      }

      BigDecimal quantity = BigDecimal.valueOf(item.getOrderedQty());
      BigDecimal subTotal = product.getPrice().multiply(quantity);

      OrderDetails orderDetail = new OrderDetails();
      orderDetail.setOrder(order);
      orderDetail.setProduct(product);
      orderDetail.setOrderedQty(item.getOrderedQty());
      orderDetail.setUnitPrice(product.getPrice());
      orderDetail.setSubTotal(subTotal);
      orderDetails.add(orderDetail);

      orderTotal = orderTotal.add(subTotal);
    }

    order.setOrderDetails(orderDetails);
    order.setOrderTotal(orderTotal);

    Order savedOrder = orderRepository.save(order);
    log.info(
        "Order {} created for user {} - {} item(s), total {}",
        savedOrder.getOrderId(),
        user.getId(),
        orderDetails.size(),
        orderTotal);
    emailService.sendOrderConfirmationEmail(savedOrder);
    return ResponseEntity.ok(OrderResponse.from(savedOrder));
  }

  public ResponseEntity<List<OrderResponse>> getOrdersForCustomer() {
    List<Order> orders =
        orderRepository.getOrderByUserId(memberIdentityHandlerService.getLoggedInMemberID());
    if (orders.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
  }

  @Transactional
  public ResponseEntity<OrderResponse> updateOrderStatus(String id, OrderStatus status) {
    memberIdentityHandlerService.requireAdmin();

    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order with id " + id + " not found"));

    OrderStatus previousStatus = order.getOrderStatus();
    if (!previousStatus.canTransitionTo(status)) {
      throw new IllegalArgumentException(
          "Invalid order status transition from " + previousStatus + " to " + status);
    }

    order.setOrderStatus(status);
    Order updatedOrder = orderRepository.save(order);
    log.info("Order {} status changed {} -> {}", id, previousStatus, status);
    return ResponseEntity.ok(OrderResponse.from(updatedOrder));
  }
}
