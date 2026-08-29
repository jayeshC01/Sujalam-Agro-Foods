package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.AuthenticationProviderException;
import com.gryffindor.excalibur.model.exception.InvalidRequestException;
import com.gryffindor.excalibur.model.request.RegisterUser;
import com.gryffindor.excalibur.model.request.UpdateProfileRequest;
import com.gryffindor.excalibur.model.response.CustomerResponse;
import com.gryffindor.excalibur.model.response.PageResponse;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private MemberIdentityHandlerService memberIdentityHandlerService;

  @Mock private FirebaseAuth firebaseAuth;

  private UserService service;

  private RegisterUser registerUser;

  private final FirebasePrincipal principal =
      new FirebasePrincipal("uid1", "jdoe@example.com", true);

  @BeforeEach
  void setUp() {
    service = new UserService(userRepository, memberIdentityHandlerService, firebaseAuth);
    registerUser = new RegisterUser("John", "Doe", "9998887777");
  }

  @Test
  void addUser_registersNewUser() {
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndStatus("jdoe@example.com", User.Status.ACTIVE))
        .thenReturn(Optional.empty());

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(userRepository)
        .save(
            argThat(
                u ->
                    u.getFirebaseUid().equals("uid1")
                        && u.getEmail().equals("jdoe@example.com")
                        && u.getRole() == Roles.USER
                        && u.getStatus() == User.Status.ACTIVE));
  }

  @Test
  void addUser_registersNewUser_whenPreviousAccountWasInactive() {
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndStatus("jdoe@example.com", User.Status.ACTIVE))
        .thenReturn(Optional.empty());

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(userRepository).save(any(User.class));
  }

  @Test
  void addUser_rejectsAlreadyRegisteredActiveFirebaseUid() {
    User activeUser = new User();
    activeUser.setStatus(User.Status.ACTIVE);
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.of(activeUser));

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(userRepository, never()).save(any());
  }

  @Test
  void addUser_rejectsBlockedFirebaseUid() {
    User blockedUser = new User();
    blockedUser.setStatus(User.Status.BLOCKED);
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.of(blockedUser));

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).contains("Your account is blocked");
    verify(userRepository, never()).save(any());
  }

  @Test
  void addUser_rejectsWhenActiveAccountWithSameEmailExists() {
    User activeUser = new User();
    activeUser.setStatus(User.Status.ACTIVE);
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(principal);
    when(userRepository.findByFirebaseUid("uid1")).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndStatus("jdoe@example.com", User.Status.ACTIVE))
        .thenReturn(Optional.of(activeUser));

    ResponseEntity<String> response = service.addUser(registerUser, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("An active account with this email already exists");
    verify(userRepository, never()).save(any());
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
  void getAllCustomers_returnsPagedResponse_whenNotEmpty() {
    User user = new User();
    user.setId("u1");
    Page<User> page =
        new PageImpl<>(
            List.of(user), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
    when(userRepository.searchCustomers(eq(null), eq(null), any(PageRequest.class)))
        .thenReturn(page);

    ResponseEntity<PageResponse<CustomerResponse>> response =
        service.getAllCustomers(null, null, 0, 10, "desc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    PageResponse<CustomerResponse> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getContent()).hasSize(1);
    assertThat(body.getTotalElements()).isEqualTo(1);
  }

  @Test
  void getCurrentCustomer_returnsLoggedInUser() {
    User user = new User();
    user.setId("u1");
    user.setFirstName("John");
    user.setEmail("jdoe@example.com");
    user.setPhoneNumber("9998887777");
    user.setRole(Roles.USER);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    ResponseEntity<CustomerResponse> response = service.getCurrentCustomer();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getId()).isEqualTo("u1");
    assertThat(response.getBody().getFirstName()).isEqualTo("John");
  }

  @Test
  void updateProfile_updatesFieldsAndSaves() {
    User user = new User();
    user.setId("u1");
    user.setFirstName("Old");
    user.setLastName("Name");
    user.setPhoneNumber("1112223333");
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    UpdateProfileRequest updateRequest =
        new UpdateProfileRequest("NewFirst", "NewLast", "9998887777");
    ResponseEntity<CustomerResponse> response = service.updateProfile(updateRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(user.getFirstName()).isEqualTo("NewFirst");
    assertThat(user.getLastName()).isEqualTo("NewLast");
    assertThat(user.getPhoneNumber()).isEqualTo("9998887777");
    verify(userRepository).save(user);
  }

  @Test
  void deleteSelf_setsActiveToFalse_deletesFirebaseUser_andSaves() throws Exception {
    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    user.setFirebaseUid("fb-u1");
    user.setStatus(User.Status.ACTIVE);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    ResponseEntity<Void> response = service.deleteSelf();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(user.isActive()).isFalse();
    assertThat(user.getStatus()).isEqualTo(User.Status.INACTIVE);
    verify(firebaseAuth).deleteUser("fb-u1");
    verify(userRepository).save(user);
  }

  @Test
  void deleteSelf_isNoOp_whenAlreadyInactive() throws Exception {
    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    user.setFirebaseUid("fb-u1");
    user.setStatus(User.Status.INACTIVE);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);

    ResponseEntity<Void> response = service.deleteSelf();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(userRepository, never()).save(any());
    verify(firebaseAuth, never()).deleteUser(any());
  }

  @Test
  void deleteSelf_succeedsSilently_whenUserNotFoundInFirebase() throws Exception {
    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    user.setFirebaseUid("fb-u1");
    user.setStatus(User.Status.ACTIVE);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    FirebaseAuthException notFoundEx = mock(FirebaseAuthException.class);
    when(notFoundEx.getMessage())
        .thenReturn("No user record found for the given identifier (user-not-found).");
    doThrow(notFoundEx).when(firebaseAuth).deleteUser("fb-u1");

    ResponseEntity<Void> response = service.deleteSelf();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(user.getStatus()).isEqualTo(User.Status.INACTIVE);
  }

  @Test
  void deleteSelf_throwsAccessDenied_whenAdminAttemptsSelfDelete() {
    User admin = new User();
    admin.setId("admin1");
    admin.setRole(Roles.ADMIN);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(admin);

    assertThatThrownBy(() -> service.deleteSelf())
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Admins cannot delete their own account");
  }

  @Test
  void disableCustomer_setsActiveToFalse_deletesFirebaseUser_andSaves() throws Exception {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    User target = new User();
    target.setId("u2");
    target.setFirebaseUid("fb-u2");
    target.setStatus(User.Status.ACTIVE);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));

    ResponseEntity<CustomerResponse> response =
        service.updateUserStatus("u2", User.Status.INACTIVE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(target.isActive()).isFalse();
    assertThat(target.getStatus()).isEqualTo(User.Status.INACTIVE);
    verify(firebaseAuth).deleteUser("fb-u2");
    verify(userRepository).save(target);
  }

  @Test
  void updateUserStatus_throwsAccessDenied_whenAdminDisablesOrBlocksThemselves() {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    assertThatThrownBy(() -> service.updateUserStatus("admin1", User.Status.INACTIVE))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Admins cannot disable or block their own account");

    assertThatThrownBy(() -> service.updateUserStatus("admin1", User.Status.BLOCKED))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Admins cannot disable or block their own account");
  }

  @Test
  void updateUserStatus_setsStatusToBlocked_disablesFirebaseUser_andSaves() throws Exception {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    User target = new User();
    target.setId("u2");
    target.setFirebaseUid("fb-u2");
    target.setStatus(User.Status.ACTIVE);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));

    ResponseEntity<CustomerResponse> response = service.updateUserStatus("u2", User.Status.BLOCKED);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(target.isBlocked()).isTrue();
    assertThat(target.getStatus()).isEqualTo(User.Status.BLOCKED);
    assertThat(target.isActive()).isFalse();
    verify(firebaseAuth).updateUser(any(UserRecord.UpdateRequest.class));
    verify(userRepository).save(target);
  }

  @Test
  void updateUserStatus_setsActiveToTrue_enablesFirebaseUser_andSaves() throws Exception {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    User target = new User();
    target.setId("u2");
    target.setFirebaseUid("fb-u2");
    target.setStatus(User.Status.BLOCKED);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));

    ResponseEntity<CustomerResponse> response = service.updateUserStatus("u2", User.Status.ACTIVE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(target.isActive()).isTrue();
    assertThat(target.getStatus()).isEqualTo(User.Status.ACTIVE);
    verify(memberIdentityHandlerService).requireAdmin();
    verify(firebaseAuth).updateUser(any(UserRecord.UpdateRequest.class));
    verify(userRepository).save(target);
  }

  @Test
  void deleteSelf_throwsAuthenticationProviderException_whenFirebaseAuthFails() throws Exception {
    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    user.setFirebaseUid("fb-u1");
    user.setStatus(User.Status.ACTIVE);
    when(memberIdentityHandlerService.getLoggedInUser()).thenReturn(user);
    doThrow(mock(FirebaseAuthException.class)).when(firebaseAuth).deleteUser("fb-u1");

    assertThatThrownBy(() -> service.deleteSelf())
        .isInstanceOf(AuthenticationProviderException.class)
        .hasMessageContaining("Failed to delete account from authentication provider");
  }

  @Test
  void updateUserStatus_throwsAuthenticationProviderException_whenDeleteFails() throws Exception {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    User target = new User();
    target.setId("u2");
    target.setFirebaseUid("fb-u2");
    target.setStatus(User.Status.ACTIVE);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));
    doThrow(mock(FirebaseAuthException.class)).when(firebaseAuth).deleteUser("fb-u2");

    assertThatThrownBy(() -> service.updateUserStatus("u2", User.Status.INACTIVE))
        .isInstanceOf(AuthenticationProviderException.class)
        .hasMessageContaining("Failed to delete account from authentication provider");
  }

  @Test
  void updateUserStatus_throwsAuthenticationProviderException_whenUpdateStatusFails()
      throws Exception {
    User admin = new User();
    admin.setId("admin1");
    when(memberIdentityHandlerService.requireAdmin()).thenReturn(admin);

    User target = new User();
    target.setId("u2");
    target.setFirebaseUid("fb-u2");
    target.setStatus(User.Status.ACTIVE);
    when(userRepository.findById("u2")).thenReturn(Optional.of(target));
    doThrow(mock(FirebaseAuthException.class))
        .when(firebaseAuth)
        .updateUser(any(UserRecord.UpdateRequest.class));

    assertThatThrownBy(() -> service.updateUserStatus("u2", User.Status.BLOCKED))
        .isInstanceOf(AuthenticationProviderException.class)
        .hasMessageContaining("Failed to update user status in authentication provider");
  }

  @Test
  void addUser_trimsInputFields() {
    FirebasePrincipal cleanPrincipal =
        new FirebasePrincipal("uid-trim", "john.doe@example.com", true);
    when(memberIdentityHandlerService.getCurrentFirebasePrincipal()).thenReturn(cleanPrincipal);
    when(userRepository.findByFirebaseUid("uid-trim")).thenReturn(Optional.empty());
    when(userRepository.findByEmailAndStatus("john.doe@example.com", User.Status.ACTIVE))
        .thenReturn(Optional.empty());

    RegisterUser userWithWhitespace = new RegisterUser("  John  ", "  Doe  ", "  9998887777  ");
    ResponseEntity<String> response = service.addUser(userWithWhitespace, Roles.USER);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(userRepository)
        .save(
            argThat(
                u ->
                    u.getFirstName().equals("John")
                        && u.getLastName().equals("Doe")
                        && u.getPhoneNumber().equals("9998887777")
                        && u.getEmail().equals("john.doe@example.com")));
  }

  @Test
  void getAllCustomers_returnsFilteredPage_whenCalled() {
    Page<User> page =
        new PageImpl<>(
            List.of(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 0);
    when(userRepository.searchCustomers(eq(User.Status.ACTIVE), eq("john"), any(PageRequest.class)))
        .thenReturn(page);

    ResponseEntity<PageResponse<CustomerResponse>> response =
        service.getAllCustomers(User.Status.ACTIVE, "john", 0, 10, "desc");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).isEmpty();
  }

  @Test
  void getAllCustomers_throwsInvalidRequest_whenInvalidSortDirection() {
    assertThatThrownBy(() -> service.getAllCustomers(null, null, 0, 10, "sideways"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid sort direction: 'sideways'");
  }
}
