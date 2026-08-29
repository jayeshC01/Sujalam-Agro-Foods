package com.gryffindor.excalibur.resources;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.request.UpdateProfileRequest;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

  private MockMvc mockMvc;

  @Mock private UserService userServiceMock;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserResource(userServiceMock))
            .setControllerAdvice(new ErrorHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  @DisplayName("Get my profile returns ok")
  void getMyProfile_returnsOk() throws Exception {
    when(userServiceMock.getCurrentCustomer())
        .thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(get("/customer/me")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Update my profile returns ok")
  void updateMyProfile_returnsOk() throws Exception {
    UpdateProfileRequest req = new UpdateProfileRequest("John", "Doe", "9998887777");
    when(userServiceMock.updateProfile(any()))
        .thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc
        .perform(
            put("/customer/me")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Delete my account returns no content")
  void deleteMyAccount_returnsNoContent() throws Exception {
    when(userServiceMock.deleteSelf()).thenReturn(ResponseEntity.noContent().build());

    mockMvc.perform(delete("/customer/me")).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Admin disable customer returns no content")
  void disableCustomer_returnsNoContent() throws Exception {
    when(userServiceMock.updateUserStatus("u1", User.Status.INACTIVE))
        .thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(delete("/admin/customer/{id}", "u1")).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Admin block customer returns ok")
  void blockCustomer_returnsOk() throws Exception {
    when(userServiceMock.updateUserStatus("u1", User.Status.BLOCKED))
        .thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(post("/admin/customer/{id}/block", "u1")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin enable customer returns ok")
  void enableCustomer_returnsOk() throws Exception {
    when(userServiceMock.updateUserStatus("u1", User.Status.ACTIVE))
        .thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(post("/admin/customer/{id}/restore", "u1")).andExpect(status().isOk());
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
    when(userServiceMock.getAllCustomers(null, null, 0, 10, "desc"))
        .thenReturn(ResponseEntity.ok(pageResponse));

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
