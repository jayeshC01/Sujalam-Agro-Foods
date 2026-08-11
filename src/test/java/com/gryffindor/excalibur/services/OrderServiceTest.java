package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.db.Product;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.InsufficientStockException;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private ProductRepository productRepository;

  @Mock private MemberIdentityHandlerService memberIdentityHandlerService;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(orderRepository, productRepository, memberIdentityHandlerService);
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
  void getAllOrders_returnsOrders_whenNotEmpty() {
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(user("u1"));
    when(orderRepository.findAll()).thenReturn(List.of(order));

    ResponseEntity<List<OrderResponse>> response = orderService.getAllOrders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).getId()).isEqualTo("o1");
    assertThat(response.getBody().get(0).getCustomer().getId()).isEqualTo("u1");
  }

  @Test
  void getAllOrders_returnsNoContent_whenEmpty() {
    when(orderRepository.findAll()).thenReturn(List.of());

    ResponseEntity<List<OrderResponse>> response = orderService.getAllOrders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void getOrderById_returnsOrder_whenRequestedByOwner() {
    User owner = user("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(owner);
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(owner);

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

    User admin = new User();
    admin.setId("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(admin);

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

    User requester = user("u2");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(requester);

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
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid order status transition");
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
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    OrderResponse body = response.getBody();
    assertThat(body.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(body.getCustomer().getId()).isEqualTo("u1");
    assertThat(body.getOrderTotal()).isEqualByComparingTo("100.00");
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
                        && o.getOrderTotal().compareTo(new BigDecimal("100.00")) == 0));
  }

  @Test
  void addOrder_throwsNotFound_whenProductMissing() {
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    when(productRepository.findById("missing")).thenReturn(Optional.empty());

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("missing");
    item.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest))
        .isInstanceOf(EntityNotFoundException.class);

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
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(0);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest))
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
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
    Product product2 = new Product();
    product2.setId("p2");
    product2.setName("Walnuts");
    product2.setPrice(new BigDecimal("20.00"));
    product2.setQty(5);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product1));
    when(productRepository.findById("p2")).thenReturn(Optional.of(product2));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);
    when(productRepository.decrementStock("p2", 3)).thenReturn(1);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p2");
    item2.setOrderedQty(3);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getOrderDetails()).hasSize(2);
    assertThat(response.getBody().getOrderTotal()).isEqualByComparingTo("110.00");
    verify(orderRepository)
        .save(
            argThat(
                o ->
                    o.getOrderDetails().size() == 2
                        && o.getOrderTotal().compareTo(new BigDecimal("110.00")) == 0));
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
    Product product2 = new Product();
    product2.setId("p2");
    product2.setName("Walnuts");
    product2.setPrice(new BigDecimal("20.00"));
    product2.setQty(1);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product1));
    when(productRepository.findById("p2")).thenReturn(Optional.of(product2));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);
    when(productRepository.decrementStock("p2", 3)).thenReturn(0);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p2");
    item2.setOrderedQty(3);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    assertThatThrownBy(() -> orderService.addOrder(orderRequest))
        .isInstanceOf(InsufficientStockException.class);

    // The whole order must be rejected (relies on @Transactional rollback for product1's
    // already-applied decrement) — no partial order should ever be persisted.
    verify(orderRepository, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
  }

  @Test
  void addOrder_reEvaluatesLiveStockPerLineItem_whenSameProductOrderedTwice() {
    // Guards against relying on a cached/stale Product read for authorization: even though the
    // same Product row is fetched twice in one request, each line item must independently call
    // the atomic decrement (which always checks live DB state), not a locally cached quantity.
    User user = user("u1");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    Product product = new Product();
    product.setId("p1");
    product.setName("Pistachios");
    product.setPrice(new BigDecimal("10.00"));
    product.setQty(2);
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));
    when(productRepository.decrementStock("p1", 1)).thenReturn(1);

    OrderRequest.ProductRequest item1 = new OrderRequest.ProductRequest();
    item1.setProductId("p1");
    item1.setOrderedQty(1);
    OrderRequest.ProductRequest item2 = new OrderRequest.ProductRequest();
    item2.setProductId("p1");
    item2.setOrderedQty(1);
    OrderRequest orderRequest = new OrderRequest(List.of(item1, item2), address());

    ResponseEntity<OrderResponse> response = orderService.addOrder(orderRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(productRepository, org.mockito.Mockito.times(2)).decrementStock("p1", 1);
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
    when(productRepository.findById("p1")).thenReturn(Optional.of(product));

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    OrderRequest orderRequestA = new OrderRequest(List.of(item), address());
    OrderRequest orderRequestB = new OrderRequest(List.of(item), address());

    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(customerA);
    when(productRepository.decrementStock("p1", 2)).thenReturn(1);
    ResponseEntity<OrderResponse> responseA = orderService.addOrder(orderRequestA);
    assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.OK);

    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(customerB);
    when(productRepository.decrementStock("p1", 2)).thenReturn(0);
    assertThatThrownBy(() -> orderService.addOrder(orderRequestB))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Insufficient stock");
  }

  @Test
  void getOrdersForCustomer_returnsOrders_whenNotEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    Order order = new Order();
    order.setOrderId("o1");
    order.setUser(user("u1"));
    when(orderRepository.getOrderByUserId("u1")).thenReturn(List.of(order));

    ResponseEntity<List<OrderResponse>> response = orderService.getOrdersForCustomer();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).getId()).isEqualTo("o1");
  }

  @Test
  void getOrdersForCustomer_returnsNoContent_whenEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    when(orderRepository.getOrderByUserId("u1")).thenReturn(List.of());

    ResponseEntity<List<OrderResponse>> response = orderService.getOrdersForCustomer();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
