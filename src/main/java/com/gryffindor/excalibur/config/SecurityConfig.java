package com.gryffindor.excalibur.config;

import com.gryffindor.excalibur.constants.Roles;
import com.gryffindor.excalibur.filters.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

  private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

  public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
    this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
}
