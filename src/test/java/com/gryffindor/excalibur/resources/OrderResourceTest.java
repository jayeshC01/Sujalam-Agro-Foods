package com.gryffindor.excalibur.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.db.Address;
import com.gryffindor.excalibur.model.db.Order;
import com.gryffindor.excalibur.model.request.OrderRequest;
import com.gryffindor.excalibur.services.OrderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
    mockMvc = MockMvcBuilders.standaloneSetup(new OrderResource(orderService)).build();
  }

  @Test
  void getOrder_returnsOk() throws Exception {
    when(orderService.getOrderById("o1")).thenReturn(ResponseEntity.ok(new Order()));

    mockMvc.perform(get("/order/{id}", "o1")).andExpect(status().isOk());
  }

  @Test
  void getOrders_returnsOk() throws Exception {
    when(orderService.getAllOrders()).thenReturn(ResponseEntity.ok(List.of(new Order())));

    mockMvc.perform(get("/orders")).andExpect(status().isOk());
  }

  @Test
  void getCustomerOrders_returnsOk() throws Exception {
    when(orderService.getOrdersForCustomer()).thenReturn(ResponseEntity.ok(List.of(new Order())));

    mockMvc.perform(get("/customer/orders")).andExpect(status().isOk());
  }

  @Test
  void createOrder_returnsOk() throws Exception {
    OrderRequest.ProductRequest item = new OrderRequest.ProductRequest();
    item.setProductId("p1");
    item.setQuantity(1);

    Address address = new Address();
    address.setRecipientName("John Doe");
    address.setPhoneNumber("9998887777");
    address.setAddressLine1("123 Main St");
    address.setCity("Surat");
    address.setState("Gujarat");
    address.setPostalCode("395007");
    address.setCountry("India");

    OrderRequest orderRequest = new OrderRequest(List.of(item), address);

    when(orderService.addOrder(any()))
        .thenReturn(new ResponseEntity<>("Order Placed Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
        .andExpect(status().isOk());
  }
}
