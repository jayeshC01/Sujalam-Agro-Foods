package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.constants.OrderStatus;
import com.gryffindor.excalibur.models.OrderRequest;
import com.gryffindor.excalibur.models.db.Order;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.repository.OrderRepository;
import com.gryffindor.excalibur.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private UserRepository userRepository;

  @Mock private MemberIdentityHandlerService memberIdentityHandlerService;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService = new OrderService(orderRepository, userRepository, memberIdentityHandlerService);
  }

  @Test
  void getAllOrders_returnsOrders_whenNotEmpty() {
    Order order = new Order();
    when(orderRepository.findAll()).thenReturn(List.of(order));

    ResponseEntity<List<Order>> response = orderService.getAllOrders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(order);
  }

  @Test
  void getAllOrders_returnsNoContent_whenEmpty() {
    when(orderRepository.findAll()).thenReturn(List.of());

    ResponseEntity<List<Order>> response = orderService.getAllOrders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void getOrderById_returnsOrder_whenFound() {
    Order order = new Order();
    when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

    ResponseEntity<Order> response = orderService.getOrderById("o1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(order);
  }

  @Test
  void getOrderById_returnsNotFound_whenMissing() {
    when(orderRepository.findById("missing")).thenReturn(Optional.empty());

    ResponseEntity<Order> response = orderService.getOrderById("missing");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void addOrder_savesOrder_forLoggedInUser() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    User user = new User();
    user.setId("u1");
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));

    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setQuantity(2);
    OrderRequest orderRequest = new OrderRequest(List.of(item), 1000L);

    ResponseEntity<String> response = orderService.addOrder(orderRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(orderRepository)
        .save(
            argThat(
                o ->
                    o.getOrderStatus() == OrderStatus.PENDING
                        && o.getUser() == user
                        && o.getOrderDetails().size() == 1));
  }

  @Test
  void getOrdersForCustomer_returnsOrders_whenNotEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    Order order = new Order();
    when(orderRepository.getOrderByUserId("u1")).thenReturn(List.of(order));

    ResponseEntity<List<Order>> response = orderService.getOrdersForCustomer();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsExactly(order);
  }

  @Test
  void getOrdersForCustomer_returnsNoContent_whenEmpty() {
    when(memberIdentityHandlerService.getLoggedInMemberID()).thenReturn("u1");
    when(orderRepository.getOrderByUserId("u1")).thenReturn(List.of());

    ResponseEntity<List<Order>> response = orderService.getOrdersForCustomer();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
