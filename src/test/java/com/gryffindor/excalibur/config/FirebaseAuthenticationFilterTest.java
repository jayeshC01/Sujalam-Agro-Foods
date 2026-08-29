package com.gryffindor.excalibur.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.gryffindor.excalibur.model.constants.Roles;
import com.gryffindor.excalibur.model.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

class FirebaseAuthenticationFilterTest {

  private FirebaseAuth firebaseAuth;
  private UserRepository userRepository;
  private HandlerExceptionResolver resolver;
  private FirebaseAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    firebaseAuth = mock(FirebaseAuth.class);
    userRepository = mock(UserRepository.class);
    resolver = mock(HandlerExceptionResolver.class);
    filter = new FirebaseAuthenticationFilter(firebaseAuth, userRepository, resolver);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilter_populatesSecurityContext_whenValidTokenProvided() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    FirebaseToken token = mock(FirebaseToken.class);
    when(token.getUid()).thenReturn("uid-1");
    when(token.getEmail()).thenReturn("user@example.com");
    when(token.isEmailVerified()).thenReturn(true);
    when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(token);

    User user = new User();
    user.setRole(Roles.ADMIN);
    when(userRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(user));

    filter.doFilter(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.isAuthenticated()).isTrue();
    assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_clearsSecurityContext_whenFirebaseAuthExceptionOccurs() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(firebaseAuth.verifyIdToken("invalid-token")).thenThrow(mock(FirebaseAuthException.class));

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_delegatesToResolver_whenUnexpectedExceptionOccurs() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token-db-down");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    FirebaseToken token = mock(FirebaseToken.class);
    when(token.getUid()).thenReturn("uid-1");
    when(token.getEmail()).thenReturn("user@example.com");
    when(token.isEmailVerified()).thenReturn(true);
    when(firebaseAuth.verifyIdToken("token-db-down")).thenReturn(token);

    RuntimeException dbError = new RuntimeException("Database connection failure");
    when(userRepository.findByFirebaseUid("uid-1")).thenThrow(dbError);

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(resolver).resolveException(request, response, null, dbError);
    verify(filterChain, never()).doFilter(request, response);
  }
}
