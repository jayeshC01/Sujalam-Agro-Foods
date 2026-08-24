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
import java.math.RoundingMode;
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

  public static final BigDecimal STANDARD_DELIVERY_CHARGE = new BigDecimal("100.00");
  public static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("500.00");

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

  @Transactional(readOnly = true)
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

  @Transactional(readOnly = true)
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

    BigDecimal subTotal = BigDecimal.ZERO;
    BigDecimal totalTax = BigDecimal.ZERO;
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
      BigDecimal itemSubTotal = product.getPrice().multiply(quantity);

      OrderDetails orderDetail = new OrderDetails();
      orderDetail.setOrder(order);
      orderDetail.setProduct(product);
      orderDetail.setOrderedQty(item.getOrderedQty());
      orderDetail.setUnitPrice(product.getPrice());
      orderDetail.setSubTotal(itemSubTotal);
      orderDetails.add(orderDetail);

      subTotal = subTotal.add(itemSubTotal);

      // Reverse calculate inclusive GST
      BigDecimal rate = product.getGstRate();
      if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal divisor = BigDecimal.ONE.add(rate);
        BigDecimal itemTax =
            itemSubTotal
                .multiply(rate)
                .divide(divisor, 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        totalTax = totalTax.add(itemTax);
      }
    }

    // Free delivery for orders >= ₹500, otherwise ₹100 standard delivery charge
    BigDecimal deliveryCharge =
        subTotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : STANDARD_DELIVERY_CHARGE.setScale(2, RoundingMode.HALF_UP);

    // Product prices already include GST, so grandTotal = subTotal + deliveryCharge
    BigDecimal grandTotal = subTotal.add(deliveryCharge).setScale(2, RoundingMode.HALF_UP);

    order.setOrderDetails(orderDetails);
    order.setSubTotal(subTotal.setScale(2, RoundingMode.HALF_UP));
    order.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
    order.setDeliveryCharge(deliveryCharge);
    order.setGrandTotal(grandTotal);

    Order savedOrder = orderRepository.save(order);
    log.info(
        "Order {} created for user {} - {} item(s), subTotal {}, tax {}, delivery {}, grandTotal {}",
        savedOrder.getOrderId(),
        user.getId(),
        orderDetails.size(),
        order.getSubTotal(),
        order.getTaxAmount(),
        order.getDeliveryCharge(),
        order.getGrandTotal());
    emailService.sendOrderConfirmationEmail(savedOrder);
    return ResponseEntity.ok(OrderResponse.from(savedOrder));
  }

  @Transactional(readOnly = true)
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

    return ResponseEntity.ok(OrderResponse.from(applyStatusChange(order, status)));
  }

  /**
   * Lets a customer call off their own order. Orders are never deleted - cancellation is a status
   * change, so the order stays queryable as business and audit history and its line items keep
   * pointing at the products they were bought at.
   */
  @Transactional
  public ResponseEntity<OrderResponse> cancelOrder(String id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order with id " + id + " not found"));

    User currentUser = memberIdentityHandlerService.getLoggedInUser();
    if (currentUser.getRole() != Roles.ADMIN
        && !order.getUser().getId().equals(currentUser.getId())) {
      throw new AccessDeniedException("You are not allowed to cancel this order");
    }

    return ResponseEntity.ok(OrderResponse.from(applyStatusChange(order, OrderStatus.CANCELED)));
  }

  /**
   * Shared transition path for both the admin status update and a customer cancellation: validates
   * the move against {@link OrderStatus#canTransitionTo}, puts stock back on the way into CANCELED,
   * and persists. Callers are responsible for authorization before calling this.
   */
  private Order applyStatusChange(Order order, OrderStatus status) {
    OrderStatus previousStatus = order.getOrderStatus();
    if (!previousStatus.canTransitionTo(status)) {
      throw new IllegalArgumentException(
          "Invalid order status transition from " + previousStatus + " to " + status);
    }

    if (status == OrderStatus.CANCELED) {
      restoreStock(order);
    }

    order.setOrderStatus(status);
    Order updatedOrder = orderRepository.save(order);
    log.info("Order {} status changed {} -> {}", order.getOrderId(), previousStatus, status);
    return updatedOrder;
  }

  private void restoreStock(Order order) {
    if (order.getOrderDetails() == null) {
      return;
    }

    for (OrderDetails item : order.getOrderDetails()) {
      String productId = item.getProduct().getId();
      int updatedRows = productRepository.incrementStock(productId, item.getOrderedQty());
      if (updatedRows == 0) {
        log.warn(
            "Could not restore {} unit(s) of product {} while canceling order {} - product row not"
                + " found",
            item.getOrderedQty(),
            productId,
            order.getOrderId());
      } else {
        log.info(
            "Restored {} unit(s) of product {} from canceled order {}",
            item.getOrderedQty(),
            productId,
            order.getOrderId());
      }
    }
  }
}
