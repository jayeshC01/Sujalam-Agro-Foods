package com.gryffindor.excalibur.config;

import com.gryffindor.excalibur.model.constants.Roles;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

  private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
  private final HandlerExceptionResolver resolver;

  public SecurityConfig(
      FirebaseAuthenticationFilter firebaseAuthenticationFilter,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    this.resolver = resolver;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
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
                    // Public product catalog browsing.
                    .requestMatchers(HttpMethod.GET, "/product/**", "/products")
                    .permitAll()
                    // Admin account creation is bootstrap-only: not exposed publicly, only an
                    // existing admin may create another admin (see /admin/** rule below).
                    .requestMatchers("/admin/**")
                    .hasRole(Roles.ADMIN.name())
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
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
