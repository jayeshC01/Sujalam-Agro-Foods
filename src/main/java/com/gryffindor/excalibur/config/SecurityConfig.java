package com.gryffindor.excalibur.config;

import com.gryffindor.excalibur.model.constants.Roles;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

  private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
  private final RateLimitingFilter rateLimitingFilter;
  private final HandlerExceptionResolver resolver;
  private final String allowedOrigins;
  private final String adminAllowedOrigins;

  public SecurityConfig(
      FirebaseAuthenticationFilter firebaseAuthenticationFilter,
      RateLimitingFilter rateLimitingFilter,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
      @Value("${app.cors.allowed-origins}") String allowedOrigins,
      @Value("${app.cors.admin-allowed-origins}") String adminAllowedOrigins) {
    this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    this.rateLimitingFilter = rateLimitingFilter;
    this.resolver = resolver;
    this.allowedOrigins = allowedOrigins;
    this.adminAllowedOrigins = adminAllowedOrigins;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/product/**", "/products")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole(Roles.ADMIN.name())
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(rateLimitingFilter, FirebaseAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    // Admin CORS policy for /admin/**
    CorsConfiguration adminConfig = new CorsConfiguration();
    List<String> adminOrigins =
        Arrays.stream(adminAllowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    adminConfig.setAllowedOriginPatterns(adminOrigins);
    adminConfig.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    adminConfig.setAllowedHeaders(List.of("*"));
    adminConfig.setExposedHeaders(
        List.of(
            "X-Request-Id",
            "X-RateLimit-Remaining",
            "X-RateLimit-Retry-After-Seconds",
            "Retry-After",
            "Authorization"));
    adminConfig.setAllowCredentials(true);
    adminConfig.setMaxAge(3600L);
    source.registerCorsConfiguration("/admin/**", adminConfig);

    // Public CORS policy
    CorsConfiguration publicConfig = new CorsConfiguration();
    List<String> publicOrigins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    publicConfig.setAllowedOriginPatterns(publicOrigins);
    publicConfig.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    publicConfig.setAllowedHeaders(List.of("*"));
    publicConfig.setExposedHeaders(
        List.of(
            "X-Request-Id",
            "X-RateLimit-Remaining",
            "X-RateLimit-Retry-After-Seconds",
            "Retry-After",
            "Authorization"));
    publicConfig.setAllowCredentials(true);
    publicConfig.setMaxAge(3600L);
    source.registerCorsConfiguration("/**", publicConfig);

    return source;
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) ->
        resolver.resolveException(request, response, null, authException);
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) ->
        resolver.resolveException(request, response, null, accessDeniedException);
  }
}
