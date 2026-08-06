package com.gryffindor.excalibur.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

class JwtUtilsTest {

  @Test
  void generateToken_and_extractUsername_roundTrip() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("jdoe");

    String token = JwtUtils.generateToken(authentication);

    assertThat(token).isNotBlank();
    assertThat(JwtUtils.getUsernameFromJWT(token)).isEqualTo("jdoe");
  }

  @Test
  void validateToken_returnsTrue_forFreshToken() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("jdoe");
    String token = JwtUtils.generateToken(authentication);

    assertThat(JwtUtils.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_withAuthentication_matchesSubject() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn("jdoe");
    String token = JwtUtils.generateToken(authentication);

    assertThat(JwtUtils.validateToken(token, authentication)).isTrue();
  }

  @Test
  void validateToken_withAuthentication_mismatchedSubjectReturnsFalse() {
    Authentication tokenOwner = mock(Authentication.class);
    when(tokenOwner.getName()).thenReturn("jdoe");
    String token = JwtUtils.generateToken(tokenOwner);

    Authentication otherUser = mock(Authentication.class);
    when(otherUser.getName()).thenReturn("someone-else");

    assertThat(JwtUtils.validateToken(token, otherUser)).isFalse();
  }

  @Test
  void validateToken_withMalformedToken_throws() {
    assertThatThrownBy(() -> JwtUtils.validateToken("not-a-valid-jwt"))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  void validateTokenWithAuthentication_withMalformedToken_throws() {
    Authentication authentication = mock(Authentication.class);

    assertThatThrownBy(() -> JwtUtils.validateToken("not-a-valid-jwt", authentication))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }
}
