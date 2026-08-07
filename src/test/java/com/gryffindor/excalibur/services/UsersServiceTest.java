package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.authentication.FirebasePrincipal;
import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.models.CustomerResponse;
import com.gryffindor.excalibur.models.RegisterUser;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.LocalDate;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private MemberIdentityHandlerService memberIdentityHandlerService;

  @Mock private Validator validator;

  private usersService service;

  private RegisterUser registerUser;

  private final FirebasePrincipal principal =
      new FirebasePrincipal("uid1", "jdoe@example.com", true);

  @BeforeEach
  void setUp() {
    service = new usersService(userRepository, memberIdentityHandlerService, validator);
    registerUser = new RegisterUser("John", "Doe", "9998887777", LocalDate.of(1990, 1, 1));
  }

  @Test
  void addUser_registersNewUser() {
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.empty());
    when(validator.validate(any(User.class))).thenReturn(Set.of());

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(userRepository)
        .save(
            argThat(
                u ->
                    u.getFirebaseUid().equals("uid1")
                        && u.getEmail().equals("jdoe@example.com")
                        && u.getRole() == Roles.USER));
  }

  @Test
  void addUser_rejectsAlreadyRegisteredFirebaseUid() {
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.of(new User()));

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(userRepository, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void addUser_throws_whenViolationsExist() {
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.empty());
    ConstraintViolation<User> violation = mock(ConstraintViolation.class);
    when(validator.validate(any(User.class))).thenReturn(Set.of(violation));

    assertThatThrownBy(() -> service.addUser(registerUser, Roles.USER))
        .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void getCustomer_returnsUser_whenRequestingOwnProfile() {
    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));

    ResponseEntity<CustomerResponse> response = service.getCustomer("u1");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("u1");
  }

  @Test
  void getCustomer_returnsUser_whenAdminRequestsAnotherProfile() {
    User admin = new User();
    admin.setId("admin1");
    admin.setRole(Roles.ADMIN);
    User target = new User();
    target.setId("u2");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(admin);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));

    ResponseEntity<CustomerResponse> response = service.getCustomer("u2");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("u2");
  }

  @Test
  void getCustomer_throwsAccessDenied_whenRequestingAnotherProfileAsNonAdmin() {
    User requester = new User();
    requester.setId("u1");
    requester.setRole(Roles.USER);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(requester);

    assertThatThrownBy(() -> service.getCustomer("u2")).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getCustomer_throws_whenNotFound() {
    User requester = new User();
    requester.setId("missing");
    requester.setRole(Roles.USER);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(requester);
    when(userRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCustomer("missing"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getAllCustomers_returnsList_whenNotEmpty() {
    User user = new User();
    user.setId("u1");
    when(userRepository.findAll()).thenReturn(List.of(user));

    ResponseEntity<List<CustomerResponse>> response = service.getAllCustomers();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  void getAllCustomers_throws_whenEmpty() {
    when(userRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> service.getAllCustomers()).isInstanceOf(EntityNotFoundException.class);
  }
}
