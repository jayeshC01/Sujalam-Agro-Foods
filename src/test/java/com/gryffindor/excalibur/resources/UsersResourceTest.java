package com.gryffindor.excalibur.resources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.CustomerResponse;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.services.usersService;
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
class UsersResourceTest {

  @Mock private usersService usersServiceMock;

  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new usersResource(usersServiceMock)).build();
  }

  @Test
  void getCustomer_returnsOk() throws Exception {
    when(usersServiceMock.getCustomer("u1")).thenReturn(ResponseEntity.ok(new CustomerResponse()));

    mockMvc.perform(get("/customer/{id}", "u1")).andExpect(status().isOk());
  }

  @Test
  void getCustomers_returnsOk() throws Exception {
    when(usersServiceMock.getAllCustomers())
        .thenReturn(ResponseEntity.ok(List.of(new CustomerResponse())));

    mockMvc.perform(get("/customers")).andExpect(status().isOk());
  }

  @Test
  void registerCustomer_returnsOk() throws Exception {
    RegisterUser registerUser =
        new RegisterUser("John", "Doe", "9998887777", LocalDate.of(1990, 1, 1));

    when(usersServiceMock.addUser(any(), eq(Roles.USER)))
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

    when(usersServiceMock.addUser(any(), eq(Roles.ADMIN)))
        .thenReturn(new ResponseEntity<>("Registered Successfully", HttpStatus.OK));

    mockMvc
        .perform(
            post("/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerUser)))
        .andExpect(status().isOk());
  }
}
