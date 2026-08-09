package com.gryffindor.excalibur.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.services.UserService;
import java.time.LocalDate;
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
class UserResourceTest {

  @Mock private UserService userServiceMock;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UserResource(userServiceMock)).build();
  }

  @Test
  void getCustomer_returnsOk() throws Exception {
    when(userServiceMock.getCustomer("u1")).thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(get("/customer/{id}", "u1")).andExpect(status().isOk());
  }

  @Test
  void getCustomers_returnsOk() throws Exception {
    when(userServiceMock.getAllCustomers())
        .thenReturn(ResponseEntity.ok(List.of(new CustomerResponse())));

    mockMvc.perform(get("/customers")).andExpect(status().isOk());
  }

  @Test
  void registerCustomer_returnsOk() throws Exception {
    RegisterUser registerUser =
        new RegisterUser("John", "Doe", "9998887777", LocalDate.of(1990, 1, 1));

    when(userServiceMock.addUser(any(), eq(Roles.USER)))
        .thenReturn(new ResponseEntity<>("Registered Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/customer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isOk());
  }

  @Test
  void registerAdmin_returnsOk() throws Exception {
    RegisterUser registerUser =
        new RegisterUser("Ad", "Min", "9998887777", LocalDate.of(1985, 5, 5));

    when(userServiceMock.addUser(any(), eq(Roles.ADMIN)))
        .thenReturn(new ResponseEntity<>("Registered Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isOk());
  }
}
