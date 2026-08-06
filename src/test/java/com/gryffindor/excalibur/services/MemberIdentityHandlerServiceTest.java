package com.gryffindor.excalibur.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gryffindor.excalibur.models.db.User;
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

  @Test
  void getLoggedInMemberID_returnsId_whenUserExists() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("jdoe");
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(context);

    User user = new User();
    user.setId("u1");
    when(userRepository.findByUserName("jdoe")).thenReturn(Optional.of(user));

    assertThat(memberIdentityHandlerService.getLoggedInMemberID()).isEqualTo("u1");
  }

  @Test
  void getLoggedInMemberID_throws_whenUserMissing() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("ghost");
    SecurityContext context = mock(SecurityContext.class);
    when(context.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(context);

    when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> memberIdentityHandlerService.getLoggedInMemberID())
        .isInstanceOf(NoSuchElementException.class);
  }
}
