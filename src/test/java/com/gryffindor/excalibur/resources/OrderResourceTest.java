package com.gryffindor.excalibur.resources;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.constants.OrderStatus;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.model.request.UpdateOrderStatusRequest;
import com.gryffindor.excalibur.model.response.OrderResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.OrderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class OrderResourceTest {

  @Mock private OrderService orderService;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new OrderResource(orderService))
            .setControllerAdvice(new ErrorHandler())
            .build();
  }

  private OrderRequest.ProductRequest validItem() {
    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setOrderedQty(2);
    return item;
  }

  private Address validAddress() {
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

  private OrderResponse sampleOrderResponse() {
    return OrderResponse.builder().id("o1").build();
  }

  @Test
  @DisplayName("Get order by id returns ok")
  void getOrder_returnsOk() throws Exception {
    when(orderService.getOrderById("o1")).thenReturn(ResponseEntity.ok(sampleOrderResponse()));

    mockMvc.perform(get("/order/{id}", "o1")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get all orders returns ok for admin with default parameters")
  void getOrders_returnsOk() throws Exception {
    PageResponse<OrderResponse> pageResponse =
        PageResponse.<OrderResponse>builder()
            .content(List.of(sampleOrderResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();
    when(orderService.getAllOrders(null, null, null, 0, 10, "createdAt", "desc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc.perform(get("/admin/orders")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get all orders returns ok with custom filters and sorting")
  void getOrders_withFiltersAndSorting_returnsOk() throws Exception {
    PageResponse<OrderResponse> pageResponse =
        PageResponse.<OrderResponse>builder()
            .content(List.of(sampleOrderResponse()))
            .page(1)
            .size(20)
            .totalElements(1)
            .totalPages(1)
            .first(false)
            .last(true)
            .build();
    when(orderService.getAllOrders(
            OrderStatus.PENDING,
            "u1",
            java.time.LocalDate.of(2026, 8, 29),
            1,
            20,
            "updatedAt",
            "asc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc
        .perform(
            get("/admin/orders")
                .param("status", "PENDING")
                .param("customerId", "u1")
                .param("createdAt", "2026-08-29")
                .param("page", "1")
                .param("size", "20")
                .param("sortBy", "updatedAt")
                .param("sort", "asc"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get orders for the logged in customer returns ok")
  void getCustomerOrders_returnsOk() throws Exception {
    PageResponse<OrderResponse> pageResponse =
        PageResponse.<OrderResponse>builder()
            .content(List.of(sampleOrderResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();
    when(orderService.getOrdersForCustomer(0, 10)).thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc.perform(get("/orders")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("valid order request with Idempotency-Key header returns 201 Created")
  void createOrder_withHeader_passesIdempotencyKey() throws Exception {
    OrderRequest orderRequest = new OrderRequest(List.of(validItem()), validAddress());

    when(orderService.addOrder(any(), org.mockito.ArgumentMatchers.eq("key-xyz-123")))
        .thenReturn(new ResponseEntity<>(sampleOrderResponse(), HttpStatus.CREATED));

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "key-xyz-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isCreated());

    verify(orderService).addOrder(any(), org.mockito.ArgumentMatchers.eq("key-xyz-123"));
  }

  @Test
  @DisplayName("order request without Idempotency-Key header returns 400 Bad Request")
  void createOrder_withoutHeader_returnsBadRequest() throws Exception {
    OrderRequest orderRequest = new OrderRequest(List.of(validItem()), validAddress());

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));

    verify(orderService, never()).addOrder(any(), any());
  }

  @Test
  @DisplayName("Admin can update order status")
  void updateOrderStatus_returnsOk() throws Exception {
    UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.COMPLETED);

    when(orderService.updateOrderStatus("o1", OrderStatus.COMPLETED))
        .thenReturn(ResponseEntity.ok(sampleOrderResponse()));

    mockMvc
        .perform(
            patch("/admin/orders/o1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(orderService).updateOrderStatus("o1", OrderStatus.COMPLETED);
  }

  @Test
  @DisplayName("Customer can cancel their own order")
  void cancelOrder_returnsOk() throws Exception {
    when(orderService.cancelOrder("o1")).thenReturn(ResponseEntity.ok(sampleOrderResponse()));

    mockMvc.perform(post("/orders/o1/cancel")).andExpect(status().isOk());

    verify(orderService).cancelOrder("o1");
  }

  @Test
  @DisplayName("Cancelling an order in a non-cancellable state returns 400")
  void cancelOrder_returnsBadRequest_forInvalidTransition() throws Exception {
    when(orderService.cancelOrder("o1"))
        .thenThrow(new IllegalArgumentException("Invalid order status transition"));

    mockMvc
        .perform(post("/orders/o1/cancel"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Invalid order status transition")));
  }

  @Test
  @DisplayName("Request with empty product list is rejected")
  void emptyProductList_isRejected() throws Exception {
    OrderRequest orderRequest = new OrderRequest(List.of(), validAddress());

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("product")));

    verify(orderService, never()).addOrder(any(), any());
  }

  @Test
  @DisplayName("Request with blank product id is rejected")
  void blankProductId_isRejected() throws Exception {
    OrderRequest.ProductRequest item = validItem();
    item.setProductId(" ");
    OrderRequest orderRequest = new OrderRequest(List.of(item), validAddress());

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("productId")));

    verify(orderService, never()).addOrder(any(), any());
  }

  @Test
  @DisplayName("Request with non-positive quantity is rejected")
  void nonPositiveQuantity_isRejected() throws Exception {
    OrderRequest.ProductRequest item = validItem();
    item.setOrderedQty(0);
    OrderRequest orderRequest = new OrderRequest(List.of(item), validAddress());

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("quantity")));

    verify(orderService, never()).addOrder(any(), any());
  }

  @Test
  @DisplayName("Request with missing shipping address is rejected")
  void missingShippingAddress_isRejected() throws Exception {
    OrderRequest orderRequest = new OrderRequest(List.of(validItem()), null);

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("shippingAddress")));

    verify(orderService, never()).addOrder(any(), any());
  }

  @Test
  @DisplayName("Request with blank recipient name in shipping address is rejected")
  void blankRecipientNameInAddress_isRejected() throws Exception {
    Address address = validAddress();
    address.setRecipientName(" ");
    OrderRequest orderRequest = new OrderRequest(List.of(validItem()), address);

    mockMvc
        .perform(
            post("/orders")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("recipientName")));

    verify(orderService, never()).addOrder(any(), any());
  }
}
