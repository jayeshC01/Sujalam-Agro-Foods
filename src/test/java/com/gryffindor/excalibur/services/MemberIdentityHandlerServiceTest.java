package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.model.exception.AccountDisabledException;
import com.gryffindor.excalibur.model.exception.EmailNotVerifiedException;
import com.gryffindor.excalibur.model.exception.UserNotRegisteredException;
import com.gryffindor.excalibur.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MemberIdentityHandlerServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private FirebaseAuth firebaseAuth;

  private MemberIdentityHandlerService memberIdentityHandlerService;

  @BeforeEach
  void setUp() {
    memberIdentityHandlerService = new MemberIdentityHandlerService(userRepository, firebaseAuth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void withPrincipal(FirebasePrincipal principal) {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(principal);
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(context);
  }

  @Test
  void getLoggedInMemberID_returnsId_whenUserExists() {
    withPrincipal(new FirebasePrincipal("uid-1", "jdoe@example.com", true));

    User user = new User();
    user.setId("u1");
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    assertThat(memberIdentityHandlerService.getLoggedInMemberID()).isEqualTo("u1");
  }

  @Test
  void getLoggedInMemberID_throws_whenUserMissing() {
    withPrincipal(new FirebasePrincipal("uid-ghost", "ghost@example.com", true));

    when(userRepository.findByFirebaseUid("uid-ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> memberIdentityHandlerService.getLoggedInMemberID())
        .isInstanceOf(UserNotRegisteredException.class);
  }

  @Test
  void getLoggedInUser_returnsUser_whenUserExists() {
    withPrincipal(new FirebasePrincipal("uid-1", "jdoe@example.com", true));

    User user = new User();
    user.setId("u1");
    user.setFirstName("John");
    user.setEmail("jdoe@example.com");
    user.setPhoneNumber("9998887777");
    user.setRole(Roles.USER);
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    User result = memberIdentityHandlerService.getLoggedInUser();

    assertThat(result).isEqualTo(user);
  }

  @Test
  void getLoggedInUser_throwsUserNotRegistered_whenUserNotFound() {
    withPrincipal(new FirebasePrincipal("uid-ghost", "ghost@example.com", true));
    when(userRepository.findByFirebaseUid("uid-ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> memberIdentityHandlerService.getLoggedInUser())
        .isInstanceOf(UserNotRegisteredException.class)
        .hasMessageContaining("complete registration");
  }

  @Test
  void getLoggedInUser_throwsAccountDisabled_whenUserIsDeactivated() {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    user.setStatus(User.Status.INACTIVE);
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> memberIdentityHandlerService.getLoggedInUser())
        .isInstanceOf(AccountDisabledException.class)
        .hasMessageContaining("account has been deactivated");
  }

  @Test
  void getLoggedInUser_throwsAccountDisabled_whenUserIsBlocked() {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    user.setStatus(User.Status.BLOCKED);
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> memberIdentityHandlerService.getLoggedInUser())
        .isInstanceOf(AccountDisabledException.class)
        .hasMessageContaining("account has been blocked by an administrator");
  }

  @Test
  void getCurrentFirebasePrincipal_returnsPrincipal_fromSecurityContext() {
    FirebasePrincipal principal = new FirebasePrincipal("uid-1", "jdoe@example.com", true);
    withPrincipal(principal);

    assertThat(memberIdentityHandlerService.getCurrentFirebasePrincipal()).isEqualTo(principal);
  }

  @Test
  void isAdmin_returnsTrue_whenUserIsAdmin() {
    withPrincipal(new FirebasePrincipal("uid-admin", "admin@example.com", true));

    User admin = new User();
    admin.setId("a1");
    admin.setRole(Roles.ADMIN);
    when(userRepository.findByFirebaseUid("uid-admin")).thenReturn(Optional.of(admin));

    assertThat(memberIdentityHandlerService.isAdmin()).isTrue();
  }

  @Test
  void isAdmin_returnsFalse_whenUserIsNotAdmin() {
    withPrincipal(new FirebasePrincipal("uid-user", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    when(userRepository.findByFirebaseUid("uid-user")).thenReturn(Optional.of(user));

    assertThat(memberIdentityHandlerService.isAdmin()).isFalse();
  }

  @Test
  void isOwner_returnsTrue_whenOwnerIdMatchesLoggedInMemberID() {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    assertThat(memberIdentityHandlerService.isOwner("u1")).isTrue();
  }

  @Test
  void isOwner_returnsFalse_whenOwnerIdDoesNotMatch() {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    assertThat(memberIdentityHandlerService.isOwner("u2")).isFalse();
  }

  @Test
  void isOwner_returnsFalse_whenOwnerIdIsNull() {
    assertThat(memberIdentityHandlerService.isOwner(null)).isFalse();
  }

  @Test
  void requireAdmin_returnsUser_whenUserIsAdmin() {
    withPrincipal(new FirebasePrincipal("uid-admin", "admin@example.com", true));

    User admin = new User();
    admin.setId("a1");
    admin.setRole(Roles.ADMIN);
    when(userRepository.findByFirebaseUid("uid-admin")).thenReturn(Optional.of(admin));

    assertThat(memberIdentityHandlerService.requireAdmin()).isEqualTo(admin);
  }

  @Test
  void requireAdmin_throwsAccessDenied_whenUserIsNotAdmin() {
    withPrincipal(new FirebasePrincipal("uid-user", "user@example.com", true));

    User user = new User();
    user.setId("u1");
    user.setRole(Roles.USER);
    when(userRepository.findByFirebaseUid("uid-user")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> memberIdentityHandlerService.requireAdmin())
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("You are not allowed to access this resource");
  }

  @Test
  void requireVerifiedEmail_doesNotThrow_whenEmailIsVerified() {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", true));

    memberIdentityHandlerService.requireVerifiedEmail();
  }

  @Test
  void requireVerifiedEmail_throwsEmailNotVerifiedException_whenEmailIsNotVerified()
      throws Exception {
    withPrincipal(new FirebasePrincipal("uid-1", "user@example.com", false));

    assertThatThrownBy(() -> memberIdentityHandlerService.requireVerifiedEmail())
        .isInstanceOf(EmailNotVerifiedException.class)
        .hasMessageContaining(
            "Email is not verified. Please verify your email address and log in again.");

    verify(firebaseAuth).revokeRefreshTokens("uid-1");
  }
}
