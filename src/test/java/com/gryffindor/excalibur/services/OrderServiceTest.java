package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.OrderDetails;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.event.OrderPlacedEvent;
import com.gryffindor.excalibur.model.event.OrderStatusUpdatedEvent;
import com.gryffindor.excalibur.model.exception.IdempotencyPayloadMismatchException;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.exception.InvalidOrderStatusTransitionException;
import com.gryffindor.excalibur.model.exception.InvalidRequestException;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.repository.OrderRepository;
import com.gryffindor.excalibur.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private ProductRepository productRepository;

  @Mock private MemberIdentityHandlerService memberIdentityHandlerService;

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private ObjectMapper objectMapper = new ObjectMapper();
  private com.gryffindor.excalibur.common.IdempotencyHelper idempotencyHelper =
      new com.gryffindor.excalibur.common.IdempotencyHelper(objectMapper);
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(
            orderRepository,
            productRepository,
            memberIdentityHandlerService,
            applicationEventPublisher,
            idempotencyHelper);
    // addOrder() reads the saved order's fields back to build the response DTO; echo the
    // argument back the way a real save() would return the persisted (same) entity.
    org.mockito.Mockito.lenient()
        .when(orderRepository.save(org.mockito.Mockito.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Address address() {
    Address address = new Address();
    address.setRecipientName("John Doe");
    address.setPhoneNumber("9998887777");
    address.setAddressLine1("123 Main St");
    address.setCity("Surat");
    address.setState("Gujarat");
    address.setPostalCode("395007");
    address.setCountry("India");
    return address;
  }

  private User user(String id) {
    User user = new User();
    user.setId(id);
    user.setRole(Roles.USER);
    return user;
  }

  @Test
  void getAllOrders_returnsPagedResponse_whenNotEmpty() {
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(user("u1"));
    Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
    when(orderRepository.searchAdminOrders(
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<OrderResponse>> response =
        orderService.getAllOrders(null, null, null, 0, 10, "createdAt", "desc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PageResponse<OrderResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getContent()).hasSize(1);
    assertThat(body.getContent().get(0).getId()).isEqualTo("o1");
    assertThat(body.getContent().get(0).getCustomer().getId()).isEqualTo("u1");
    assertThat(body.getTotalElements()).isEqualTo(1);
    verify(memberIdentityHandlerService).requireAdmin();
  }

  @Test
  void getAllOrders_withFiltersAndAscSort() {
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(user("u1"));
    Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(1, 20), 1);
    java.time.LocalDate date = java.time.LocalDate.of(2026, 8, 29);
    java.time.LocalDateTime start = date.atStartOfDay();
    java.time.LocalDateTime end = date.atTime(java.time.LocalTime.MAX);
    when(orderRepository.searchAdminOrders(
            OrderStatus.PENDING,
            "u1",
            start,
            end,
            PageRequest.of(1, 20, Sort.by(Sort.Direction.ASC, "updatedAt"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<OrderResponse>> response =
        orderService.getAllOrders(OrderStatus.PENDING, "  u1  ", date, 1, 20, "updatedAt", "asc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(1);
    verify(memberIdentityHandlerService).requireAdmin();
  }

  @Test
  void getAllOrders_returnsEmptyPage_whenEmpty() {
    Page<Order> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(orderRepository.searchAdminOrders(
            null,
            null,
            null,
            null,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<OrderResponse>> response =
        orderService.getAllOrders(null, null, null, 0, 10, "createdAt", "desc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEmpty();
    assertThat(response.getBody().getTotalElements()).isEqualTo(0);
  }

  @Test
  void getAllOrders_throwsInvalidRequest_whenSortByIsInvalid() {
    assertThatThrownBy(
            () -> orderService.getAllOrders(null, null, null, 0, 10, "invalidField", "desc"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(
            "Invalid sortBy value: 'invalidField'. Allowed values: createdAt, updatedAt");
  }

  @Test
  void getAllOrders_throwsInvalidRequest_whenSortDirectionIsInvalid() {
    assertThatThrownBy(
            () -> orderService.getAllOrders(null, null, null, 0, 10, "createdAt", "invalidDir"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid sort direction: 'invalidDir'");
  }

  @Test
  void getOrderById_returnsOrder_whenRequestedByOwner() {
    User owner = user("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(owner);
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(true);

    ResponseEntity<OrderResponse> response = orderService.getOrderById("o1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("o1");
    assertThat(response.getBody().getCustomer().getId()).isEqualTo("u1");
  }

  @Test
  void getOrderById_returnsOrder_whenRequestedByAdmin() {
    User owner = user("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(owner);
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(true);

    ResponseEntity<OrderResponse> response = orderService.getOrderById("o1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("o1");
    assertThat(response.getBody().getCustomer().getId()).isEqualTo("u1");
  }

  @Test
  void getOrderById_throwsAccessDenied_whenRequestedByAnotherCustomer() {
    User owner = user("u1");
    Order order = new Order();
    order.setUser(owner);
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(false);

    assertThatThrownBy(() -> orderService.getOrderById("o1"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getOrderById_throws_whenMissing() {
    when(orderRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getOrderById("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateOrderStatus_updatesExistingOrder_whenRequestedByAdmin() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(orderRepository.save(order)).thenReturn(order);

    ResponseEntity<OrderResponse> response =
        orderService.updateOrderStatus("o1", OrderStatus.COMPLETED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
  }

  @Test
  void updateOrderStatus_throwsAccessDenied_whenRequestedByNonAdmin() {
    when(memberIdentityHandlerService.requireAdmin())
        .thenThrow(new AccessDeniedException("You are not allowed to access this resource"));

    assertThatThrownBy(() -> orderService.updateOrderStatus("o1", OrderStatus.COMPLETED))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updateOrderStatus_throwsForInvalidTransition() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.COMPLETED);
    order.setUser(user("u1"));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.updateOrderStatus("o1", OrderStatus.CANCELED))
        .isInstanceOf(InvalidOrderStatusTransitionException.class)
        .hasMessageContaining("Invalid order status transition");
  }

  private OrderDetails orderDetail(Product product, int qty) {
    OrderDetails detail = new OrderDetails();
    detail.setProduct(product);
    detail.setOrderedQty(qty);
    detail.setUnitPrice(product.getPrice());
    detail.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(qty)));
    return detail;
  }

  private Product product(String id, String name, String price, int qty) {
    Product product = new Product();
    product.setId(id);
    product.setName(name);
    product.setPrice(new BigDecimal(price));
    product.setQty(qty);
    product.setGstRate(new BigDecimal("0.05"));
    return product;
  }

  @Test
  void updateOrderStatus_restoresStock_whenOrderIsCanceled() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Product product = product("p1", "Cashews", "50.00", 8);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(productRepository.incrementStock("p1", 2)).thenReturn(1);

    ResponseEntity<OrderResponse> response =
        orderService.updateOrderStatus("o1", OrderStatus.CANCELED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    verify(productRepository).incrementStock("p1", 2);
    verify(applicationEventPublisher).publishEvent(any(OrderStatusUpdatedEvent.class));
  }

  @Test
  void updateOrderStatus_restoresStockPerLineItem_forMultiProductOrder() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Product product1 = product("p1", "Cashews", "50.00", 5);
    Product product2 = product("p2", "Walnuts", "20.00", 5);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product1, 1), orderDetail(product2, 3)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(productRepository.incrementStock("p1", 1)).thenReturn(1);
    when(productRepository.incrementStock("p2", 3)).thenReturn(1);

    orderService.updateOrderStatus("o1", OrderStatus.CANCELED);

    verify(productRepository).incrementStock("p1", 1);
    verify(productRepository).incrementStock("p2", 3);
    verify(applicationEventPublisher).publishEvent(any(OrderStatusUpdatedEvent.class));
  }

  @Test
  void updateOrderStatus_doesNotRestoreStock_whenOrderIsCompleted() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Product product = product("p1", "Cashews", "50.00", 8);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

    orderService.updateOrderStatus("o1", OrderStatus.COMPLETED);

    verify(productRepository, org.mockito.Mockito.never())
        .incrementStock(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
    verify(applicationEventPublisher).publishEvent(any(OrderStatusUpdatedEvent.class));
  }

  @Test
  void updateOrderStatus_doesNotRestoreStockTwice_whenCancelingAnAlreadyCanceledOrder() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Product product = product("p1", "Cashews", "50.00", 10);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.CANCELED);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.updateOrderStatus("o1", OrderStatus.CANCELED))
        .isInstanceOf(InvalidOrderStatusTransitionException.class);

    verify(productRepository, org.mockito.Mockito.never())
        .incrementStock(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
  }

  @Test
  void updateOrderStatus_stillCancels_whenStockRestoreAffectsNoRows() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Product product = product("p1", "Cashews", "50.00", 8);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(productRepository.incrementStock("p1", 2)).thenReturn(0);

    ResponseEntity<OrderResponse> response =
        orderService.updateOrderStatus("o1", OrderStatus.CANCELED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
  }

  @Test
  void updateOrderStatus_cancelsCleanly_whenOrderHasNoLineItems() {
    User admin = user("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

    ResponseEntity<OrderResponse> response =
        orderService.updateOrderStatus("o1", OrderStatus.CANCELED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
  }

  @Test
  void cancelOrder_cancelsAndRestoresStock_whenRequestedByOwner() {
    User owner = user("u1");

    Product product = product("p1", "Cashews", "50.00", 8);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(owner);
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(true);
    when(productRepository.incrementStock("p1", 2)).thenReturn(1);

    ResponseEntity<OrderResponse> response = orderService.cancelOrder("o1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    verify(productRepository).incrementStock("p1", 2);
  }

  @Test
  void cancelOrder_throwsAccessDenied_whenRequestedByAnotherCustomer() {
    User owner = user("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(owner);
    order.setOrderDetails(List.of(orderDetail(product("p1", "Cashews", "50.00", 8), 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(false);

    assertThatThrownBy(() -> orderService.cancelOrder("o1"))
        .isInstanceOf(AccessDeniedException.class);

    // Another customer's cancellation must not touch stock or the order's status.
    verify(productRepository, org.mockito.Mockito.never())
        .incrementStock(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void cancelOrder_isAllowedForAdmin_onAnotherCustomersOrder() {
    Product product = product("p1", "Cashews", "50.00", 8);
    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.PENDING);
    order.setUser(user("u1"));
    order.setOrderDetails(List.of(orderDetail(product, 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(true);
    when(productRepository.incrementStock("p1", 2)).thenReturn(1);

    ResponseEntity<OrderResponse> response = orderService.cancelOrder("o1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
  }

  @Test
  void cancelOrder_throwsForInvalidTransition_whenAlreadyCompleted() {
    User owner = user("u1");

    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.COMPLETED);
    order.setUser(owner);
    order.setOrderDetails(List.of(orderDetail(product("p1", "Cashews", "50.00", 8), 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(true);

    assertThatThrownBy(() -> orderService.cancelOrder("o1"))
        .isInstanceOf(InvalidOrderStatusTransitionException.class)
        .hasMessageContaining("Invalid order status transition");

    verify(productRepository, org.mockito.Mockito.never())
        .incrementStock(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
  }

  @Test
  void cancelOrder_throwsForInvalidTransition_whenOrderAlreadyCanceled() {
    User owner = user("u1");

    Order order = new Order();
    order.setOrderId("o1");
    order.setOrderStatus(OrderStatus.CANCELED);
    order.setUser(owner);
    order.setOrderDetails(List.of(orderDetail(product("p1", "Cashews", "50.00", 8), 2)));
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.isAdmin()).thenReturn(false);
    when(memberIdentityHandlerService.isOwner("u1")).thenReturn(true);

    assertThatThrownBy(() -> orderService.cancelOrder("o1"))
        .isInstanceOf(InvalidOrderStatusTransitionException.class);

    // Guards against a second cancellation crediting the same stock twice.
    verify(productRepository, org.mockito.Mockito.never())
        .incrementStock(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
  }

  @Test
  void cancelOrder_throwsNotFound_whenOrderMissing() {
    when(orderRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.cancelOrder("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void addOrder_computesTotalFromProductPrice_forLoggedInUser() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Cashews");
    product.setPrice(new BigDecimal("50.00"));
    product.setQty(10);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest, "test-idem-key-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderResponse body = response.getBody();
    assertThat(body.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(body.getCustomer().getId()).isEqualTo("u1");
    assertThat(body.getSubTotal()).isEqualByComparingTo("100.00");
    assertThat(body.getTaxAmount()).isEqualByComparingTo("4.76");
    assertThat(body.getDeliveryCharge()).isEqualByComparingTo("100.00");
    assertThat(body.getGrandTotal()).isEqualByComparingTo("200.00");
    assertThat(body.getOrderDetails()).hasSize(1);
    assertThat(body.getOrderDetails().get(0).getProductId()).isEqualTo("p1");
    assertThat(body.getOrderDetails().get(0).getProductName()).isEqualTo("Cashews");
    assertThat(body.getOrderDetails().get(0).getSubTotal()).isEqualByComparingTo("100.00");

    verify(orderRepository)
        .save(
            argThat(
                o ->
                    o.getOrderStatus() == OrderStatus.PENDING
                        && o.getUser() == user
                        && o.getOrderDetails().size() == 1
                        && o.getSubTotal().compareTo(new BigDecimal("100.00")) == 0
                        && o.getTaxAmount().compareTo(new BigDecimal("4.76")) == 0
                        && o.getDeliveryCharge().compareTo(new BigDecimal("100.00")) == 0
                        && o.getGrandTotal().compareTo(new BigDecimal("200.00")) == 0));
    verify(applicationEventPublisher).publishEvent(any(OrderPlacedEvent.class));
  }

  @Test
  void addOrder_throwsNotFound_whenProductMissing() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    when(productRepository.findByIdAndStatus("missing", Product.Status.ACTIVE))
        .thenReturn(Optional.empty());

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("missing");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest, "test-idem-missing"))
        .isInstanceOf(EntityNotFoundException.class);

    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }

  @Test
  void addOrder_throwsNotFound_whenProductIsInactive() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.empty());

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest, "test-idem-inactive"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Product p1 not found");

    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }

  @Test
  void addOrder_throwsInsufficientStock_whenAtomicDecrementAffectsNoRows() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Almonds");
    product.setPrice(new BigDecimal("50.00"));
    product.setQty(1);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(0);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest, "test-idem-insufficient"))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Insufficient stock");

    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }

  @Test
  void addOrder_succeeds_whenQuantityExactlyMatchesRemainingStock() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Almonds");
    product.setPrice(new BigDecimal("50.00"));
    product.setQty(2);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest, "test-idem-exact");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    verify(productRepository).decrementStock("p1", 2);
  }

  @Test
  void addOrder_decrementsEachProductIndependently_forMultiProductOrder() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product1 = new Product();
    product1.setId("p1");
    product1.setName("Cashews");
    product1.setPrice(new BigDecimal("50.00"));
    product1.setQty(5);
    product1.setGstRate(new BigDecimal("0.05"));
    Product product2 = new Product();
    product2.setId("p2");
    product2.setName("Walnuts");
    product2.setPrice(new BigDecimal("20.00"));
    product2.setQty(5);
    product2.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product1));
    when(productRepository.findByIdAndStatus("p2", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product2));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);
    when(productRepository.decrementStock("p2", 3)).thenReturn(1);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p2");
    item2.setOrderedQty(3);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "test-idem-multi-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getOrderDetails()).hasSize(2);
    assertThat(response.getBody().getSubTotal()).isEqualByComparingTo("110.00");
    assertThat(response.getBody().getTaxAmount()).isEqualByComparingTo("5.24");
    assertThat(response.getBody().getDeliveryCharge()).isEqualByComparingTo("100.00");
    assertThat(response.getBody().getGrandTotal()).isEqualByComparingTo("210.00");
    verify(orderRepository)
        .save(
            argThat(
                o ->
                    o.getOrderDetails().size() == 2
                        && o.getSubTotal().compareTo(new BigDecimal("110.00")) == 0
                        && o.getTaxAmount().compareTo(new BigDecimal("5.24")) == 0
                        && o.getDeliveryCharge().compareTo(new BigDecimal("100.00")) == 0
                        && o.getGrandTotal().compareTo(new BigDecimal("210.00")) == 0));
  }

  @Test
  void addOrder_failsWholeOrder_whenLaterItemHasInsufficientStock() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product1 = new Product();
    product1.setId("p1");
    product1.setName("Cashews");
    product1.setPrice(new BigDecimal("50.00"));
    product1.setQty(5);
    product1.setGstRate(new BigDecimal("0.05"));
    Product product2 = new Product();
    product2.setId("p2");
    product2.setName("Walnuts");
    product2.setPrice(new BigDecimal("20.00"));
    product2.setQty(1);
    product2.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product1));
    when(productRepository.findByIdAndStatus("p2", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product2));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);
    when(productRepository.decrementStock("p2", 3)).thenReturn(0);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p2");
    item2.setOrderedQty(3);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest, "test-idem-later-fail"))
        .isInstanceOf(InsufficientStockException.class);

    // The whole order must be rejected (relies on @Transactional rollback for product1's
    // already-applied decrement) — no partial order should ever be persisted.
    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }

  @Test
  void addOrder_consolidatesDuplicateProductItemsInSingleOrder() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Pistachios");
    product.setPrice(new BigDecimal("10.00"));
    product.setQty(5);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 3)).thenReturn(1);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p1");
    item2.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "test-idem-dup-item");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getOrderDetails()).hasSize(1);
    assertThat(response.getBody().getOrderDetails().get(0).getProductId()).isEqualTo("p1");
    assertThat(response.getBody().getOrderDetails().get(0).getOrderedQty()).isEqualTo(3);
    assertThat(response.getBody().getOrderDetails().get(0).getSubTotal())
        .isEqualByComparingTo("30.00");
    verify(productRepository, org.mockito.Mockito.times(1)).decrementStock("p1", 3);
  }

  @Test
  void addOrder_locksProductsInCanonicalSortedOrder_preventingDeadlocks() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product productB = product("p2", "Walnuts", "20.00", 5);
    Product productA = product("p1", "Cashews", "50.00", 5);
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(productA));
    when(productRepository.findByIdAndStatus("p2", Product.Status.ACTIVE))
        .thenReturn(Optional.of(productB));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);
    when(productRepository.decrementStock("p2", 1)).thenReturn(1);

    // Provide items in reverse alphabetical order: p2 then p1
    OrderRequest.ProductRequest itemB = new OrderRequest.ProductRequest();
    itemB.setProductId("p2");
    itemB.setOrderedQty(1);
    OrderRequest.ProductRequest itemA = new OrderRequest.ProductRequest();
    itemA.setProductId("p1");
    itemA.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(itemB, itemA), address());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "test-idem-sorted-locks");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // Verify row locks / stock decrements are strictly executed in ascending sorted order: p1 then
    // p2
    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(productRepository);
    inOrder.verify(productRepository).decrementStock("p1", 1);
    inOrder.verify(productRepository).decrementStock("p2", 1);
  }

  @Test
  void addOrder_secondConcurrentCustomer_isRejected_whenFirstExhaustsStock() {
    // Simulates two customers racing for the last 2 units of stock. The atomic
    // "UPDATE ... WHERE qty >= :quantity" is the real concurrency guard (enforced by the DB for
    // true concurrent transactions); here we simulate the outcome that guard produces: whichever
    // request's UPDATE commits first wins, and the second one's UPDATE affects zero rows.
    User customerA = user("uA");
    User customerB = user("uB");

    Product product = new Product();
    product.setId("p1");
    product.setName("Cardamom");
    product.setPrice(new BigDecimal("100.00"));
    product.setQty(2);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequestA = new OrderRequest(List.of(item), address());
    OrderRequest orderRequestB = new OrderRequest(List.of(item), address());

    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(customerA);
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);
    ResponseEntity<OrderResponse> responseA =
        orderService.addOrder(orderRequestA, "test-idem-cust-a");
    assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(customerB);
    when(productRepository.decrementStock("p1", 2)).thenReturn(0);
    assertThatThrownBy(() -> orderService.addOrder(orderRequestB, "test-idem-cust-b"))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Insufficient stock");
  }

  @Test
  void getOrdersForCustomer_returnsPagedResponse_whenNotEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(user("u1"));
    Page<Order> page =
        new PageImpl<>(
            List.of(order), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
    when(orderRepository.findByUserId(
            "u1", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<OrderResponse>> response = orderService.getOrdersForCustomer(0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PageResponse<OrderResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getContent()).hasSize(1);
    assertThat(body.getContent().get(0).getId()).isEqualTo("o1");
    assertThat(body.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getOrdersForCustomer_returnsEmptyPage_whenEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    Page<Order> page =
        new PageImpl<>(
            List.of(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 0);
    when(orderRepository.findByUserId(
            "u1", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
        .thenReturn(page);

    ResponseEntity<PageResponse<OrderResponse>> response = orderService.getOrdersForCustomer(0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEmpty();
    assertThat(response.getBody().getTotalElements()).isEqualTo(0);
  }

  @Test
  void addOrder_appliesFreeDelivery_whenSubtotalReachesThreshold() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Premium Saffron");
    product.setPrice(new BigDecimal("500.00"));
    product.setQty(5);
    product.setGstRate(new BigDecimal("0.05"));
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "test-idem-free-del");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderResponse body = response.getBody();
    assertThat(body.getSubTotal()).isEqualByComparingTo("500.00");
    assertThat(body.getTaxAmount()).isEqualByComparingTo("23.81"); // 5% inclusive of 500
    assertThat(body.getDeliveryCharge()).isEqualByComparingTo("0.00"); // Free delivery
    assertThat(body.getGrandTotal()).isEqualByComparingTo("500.00");
  }

  @Test
  void addOrder_calculatesCustomGstRate_whenProductHasCustomGstRate() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Processed Snacks");
    product.setPrice(new BigDecimal("112.00")); // Inclusive of 12% GST
    product.setQty(5);
    product.setGstRate(new BigDecimal("0.12")); // 12% GST
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "test-idem-gst-rate");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    OrderResponse body = response.getBody();
    assertThat(body.getSubTotal()).isEqualByComparingTo("112.00");
    assertThat(body.getTaxAmount()).isEqualByComparingTo("12.00"); // 112 * (0.12 / 1.12) = 12.00
    assertThat(body.getDeliveryCharge()).isEqualByComparingTo("100.00");
    assertThat(body.getGrandTotal()).isEqualByComparingTo("212.00");
  }

  @Test
  void addOrder_withExistingIdempotencyKey_replaysExistingOrderWithoutDeductingStock() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Order existingOrder = new Order();
    existingOrder.setOrderId("ord-existing-1");
    existingOrder.setUser(user);
    existingOrder.setOrderStatus(OrderStatus.PENDING);
    existingOrder.setSubTotal(new BigDecimal("100.00"));
    existingOrder.setTaxAmount(new BigDecimal("4.76"));
    existingOrder.setDeliveryCharge(new BigDecimal("100.00"));
    existingOrder.setGrandTotal(new BigDecimal("200.00"));
    existingOrder.setIdempotencyKey("idem-key-1");

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    // Set matching request hash on existing order
    try {
      String json = objectMapper.writeValueAsString(orderRequest);
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      existingOrder.setRequestHash(java.util.HexFormat.of().formatHex(hash));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    when(orderRepository.findByUserIdAndIdempotencyKey("u1", "idem-key-1"))
        .thenReturn(Optional.of(existingOrder));

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest, "idem-key-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("ord-existing-1");

    // Stock was NEVER deducted and new order was NOT saved
    verify(productRepository, org.mockito.Mockito.never())
        .decrementStock(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void addOrder_withExistingIdempotencyKeyAndDifferentPayload_throwsPayloadMismatchException() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Order existingOrder = new Order();
    existingOrder.setOrderId("ord-existing-1");
    existingOrder.setUser(user);
    existingOrder.setIdempotencyKey("idem-key-1");
    existingOrder.setRequestHash("different-original-hash-12345");

    when(orderRepository.findByUserIdAndIdempotencyKey("u1", "idem-key-1"))
        .thenReturn(Optional.of(existingOrder));

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest, "idem-key-1"))
        .isInstanceOf(IdempotencyPayloadMismatchException.class)
        .hasMessageContaining("previously used with a different request payload");
  }

  @Test
  void addOrder_handlesConcurrentInsertRaceCondition_returnsWinnerOrder() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = product("p1", "Cashews", "100.00", 5);
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    Order winnerOrder = new Order();
    winnerOrder.setOrderId("ord-winner-99");
    winnerOrder.setUser(user);
    winnerOrder.setOrderStatus(OrderStatus.PENDING);
    winnerOrder.setSubTotal(new BigDecimal("100.00"));
    winnerOrder.setTaxAmount(new BigDecimal("4.76"));
    winnerOrder.setDeliveryCharge(new BigDecimal("100.00"));
    winnerOrder.setGrandTotal(new BigDecimal("200.00"));
    winnerOrder.setIdempotencyKey("race-key-1");

    when(orderRepository.findByUserIdAndIdempotencyKey("u1", "race-key-1"))
        .thenReturn(Optional.empty()) // first check: not found
        .thenReturn(Optional.of(winnerOrder)); // second check after race: found winner order

    when(orderRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenThrow(
            new org.springframework.dao.DataIntegrityViolationException(
                "Duplicate key uk_orders_user_idempotency"));

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest, "race-key-1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getId()).isEqualTo("ord-winner-99");
  }

  @Test
  void addOrder_allowsSameIdempotencyKey_forDifferentUsers() {
    User user1 = user("u1");
    User user2 = user("u2");

    Product product = product("p1", "Cashews", "100.00", 10);
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    // User 1 places order with key-shared
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user1);
    when(orderRepository.findByUserIdAndIdempotencyKey("u1", "key-shared"))
        .thenReturn(Optional.empty());
    ResponseEntity<OrderResponse> response1 = orderService.addOrder(orderRequest, "key-shared");
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    // User 2 places order with SAME key-shared without being blocked
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user2);
    when(orderRepository.findByUserIdAndIdempotencyKey("u2", "key-shared"))
        .thenReturn(Optional.empty());
    ResponseEntity<OrderResponse> response2 = orderService.addOrder(orderRequest, "key-shared");
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    verify(orderRepository).findByUserIdAndIdempotencyKey("u1", "key-shared");
    verify(orderRepository).findByUserIdAndIdempotencyKey("u2", "key-shared");
  }

  @Test
  void addOrder_trimsLeadingAndTrailingWhitespaceFromKey() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = product("p1", "Cashews", "100.00", 5);
    when(productRepository.findByIdAndStatus("p1", Product.Status.ACTIVE))
        .thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    when(orderRepository.findByUserIdAndIdempotencyKey("u1", "key-trimmed"))
        .thenReturn(Optional.empty());

    ResponseEntity<OrderResponse> response =
        orderService.addOrder(orderRequest, "   key-trimmed   ");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    verify(orderRepository).save(argThat(o -> "key-trimmed".equals(o.getIdempotencyKey())));
  }
}
