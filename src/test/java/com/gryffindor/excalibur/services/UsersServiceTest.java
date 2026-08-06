package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private Validator validator;

  private usersService service;

  private RegisterUser registerUser;

  @BeforeEach
  void setUp() {
    service = new usersService(userRepository, validator);
    registerUser = new RegisterUser("jdoe", "secret", "John", "Doe", new Date());
  }

  @Test
  void addUser_registersNewUser() {
    when(userRepository.findByUserName("jdoe")).thenReturn(Optional.empty());
    when(validator.validate(any(User.class))).thenReturn(Set.of());

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(userRepository)
        .save(argThat(u -> u.getUserName().equals("jdoe") && u.getRole() == Roles.USER));
  }

  @Test
  void addUser_rejectsDuplicateUsername() {
    when(userRepository.findByUserName("jdoe")).thenReturn(Optional.of(new User()));

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(userRepository, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void addUser_throws_whenViolationsExist() {
    when(userRepository.findByUserName("jdoe")).thenReturn(Optional.empty());
    ConstraintViolation<User> violation = mock(ConstraintViolation.class);
    when(validator.validate(any(User.class))).thenReturn(Set.of(violation));

    assertThatThrownBy(() -> service.addUser(registerUser, Roles.USER))
        .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void getCustomer_returnsUser_whenFound() {
    User user = new User();
    user.setId("u1");
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));

    ResponseEntity<User> response = service.getCustomer("u1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(user);
  }

  @Test
  void getCustomer_throws_whenNotFound() {
    when(userRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCustomer("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getAllCustomers_returnsList_whenNotEmpty() {
    when(userRepository.findAll()).thenReturn(List.of(new User()));

    ResponseEntity<List<User>> response = service.getAllCustomers();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void getAllCustomers_throws_whenEmpty() {
    when(userRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.getAllCustomers()).isInstanceOf(EntityNotFoundException.class);
  }
}
