package com.gryffindor.excalibur.resources;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.services.UserService;
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
class UserResourceTest {

  @Mock private UserService userServiceMock;
  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserResource(userServiceMock))
            .setControllerAdvice(new ErrorHandler())
            .build();
  }

  @Test
  @DisplayName("Get customer by id returns ok")
  void getCustomer_returnsOk() throws Exception {
    when(userServiceMock.getCustomer("u1")).thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(get("/customer/{id}", "u1")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Get all customers returns ok")
  void getCustomers_returnsOk() throws Exception {
    PageResponse<CustomerResponse> pageResponse =
        PageResponse.<CustomerResponse>builder()
            .content(List.of(new CustomerResponse()))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();
    when(userServiceMock.getAllCustomers(0, 10)).thenReturn(ResponseEntity.ok(pageResponse));

    mockMvc.perform(get("/customers")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("valid customer registration is accepted")
  void registerCustomer_returnsOk() throws Exception {
    RegisterUser registerUser = new RegisterUser("John", "Doe", "9998887777");

    when(userServiceMock.addUser(any(), eq(Roles.USER)))
        .thenReturn(new ResponseEntity<>("Registered Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isOk());

    verify(userServiceMock).addUser(any(), eq(Roles.USER));
  }

  @Test
  @DisplayName("Request with blank first name on customer registration is rejected")
  void blankFirstNameOnCustomerRegistration_isRejected() throws Exception {
    RegisterUser registerUser = new RegisterUser(" ", "Doe", "9998887777");

    mockMvc
        .perform(
            post("/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("firstName")));

    verify(userServiceMock, never()).addUser(any(), any());
  }

  @Test
  @DisplayName("valid admin registration is accepted")
  void registerAdmin_returnsOk() throws Exception {
    RegisterUser registerUser = new RegisterUser("Ad", "Min", "9998887777");

    when(userServiceMock.addUser(any(), eq(Roles.ADMIN)))
        .thenReturn(new ResponseEntity<>("Registered Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isOk());

    verify(userServiceMock).addUser(any(), eq(Roles.ADMIN));
  }

  @Test
  @DisplayName("Request with blank first name on admin registration is rejected")
  void blankFirstNameOnAdminRegistration_isRejected() throws Exception {
    RegisterUser registerUser = new RegisterUser(" ", "Min", "9998887777");

    mockMvc
        .perform(
            post("/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details").value(containsString("firstName")));

    verify(userServiceMock, never()).addUser(any(), any());
  }
}
