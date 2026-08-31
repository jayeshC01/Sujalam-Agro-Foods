package com.gryffindor.excalibur.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

class SecurityConfigTest {

  private HandlerExceptionResolver resolver;
  private SecurityConfig securityConfig;

  @BeforeEach
  void setUp() {
    resolver = mock(HandlerExceptionResolver.class);
    FirebaseAuthenticationFilter filter = mock(FirebaseAuthenticationFilter.class);
    RateLimitingFilter rateLimitingFilter = mock(RateLimitingFilter.class);
    securityConfig =
        new SecurityConfig(
            filter,
            rateLimitingFilter,
            resolver,
            "http://localhost:3000",
            "http://admin.localhost:3000");
  }

  @Test
  void corsConfigurationSource_setsExpectedPoliciesForCustomerEndpoints() {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/products");

    CorsConfiguration config = source.getCorsConfiguration(request);

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://localhost:3000");
    assertThat(config.getAllowedMethods())
        .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(config.getExposedHeaders())
        .contains("X-Request-Id", "X-RateLimit-Remaining", "Retry-After");
  }

  @Test
  void corsConfigurationSource_setsExpectedPoliciesForAdminEndpoints() {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/admin/products");

    CorsConfiguration config = source.getCorsConfiguration(request);

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://admin.localhost:3000");
    assertThat(config.getAllowedMethods())
        .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(config.getExposedHeaders())
        .contains("X-Request-Id", "X-RateLimit-Remaining", "Retry-After");
  }

  @Test
  void authenticationEntryPoint_delegatesToResolver() throws Exception {
    AuthenticationEntryPoint entryPoint = securityConfig.authenticationEntryPoint();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AuthenticationException authException = mock(AuthenticationException.class);

    entryPoint.commence(request, response, authException);

    verify(resolver).resolveException(request, response, null, authException);
  }

  @Test
  void accessDeniedHandler_delegatesToResolver() throws Exception {
    AccessDeniedHandler handler = securityConfig.accessDeniedHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AccessDeniedException accessDeniedException = new AccessDeniedException("Forbidden");

    handler.handle(request, response, accessDeniedException);

    verify(resolver).resolveException(request, response, null, accessDeniedException);
  }
}
