package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.config.FirebasePrincipal;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MemberIdentityHandlerServiceTest {

  @Mock private UserRepository userRepository;

  private MemberIdentityHandlerService memberIdentityHandlerService;

  @BeforeEach
  void setUp() {
    memberIdentityHandlerService = new MemberIdentityHandlerService(userRepository);
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
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void getCurrentFirebasePrincipal_returnsPrincipal_fromSecurityContext() {
    FirebasePrincipal principal = new FirebasePrincipal("uid-1", "jdoe@example.com", true);
    withPrincipal(principal);

    assertThat(memberIdentityHandlerService.getCurrentFirebasePrincipal()).isEqualTo(principal);
  }
}
