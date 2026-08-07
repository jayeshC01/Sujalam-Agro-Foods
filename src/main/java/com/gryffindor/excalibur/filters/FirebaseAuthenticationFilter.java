package com.gryffindor.excalibur.filters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.gryffindor.excalibur.authentication.FirebasePrincipal;
import com.gryffindor.excalibur.models.db.User;
import com.gryffindor.excalibur.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the Firebase ID token sent as a Bearer token and populates the security context.
 * Authorities come from the local {@link User#getRole()} when a matching profile exists; a verified
 * token with no local profile yet is still authenticated (no authorities), which is enough to reach
 * the "complete registration" endpoint.
 */
@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

  private final FirebaseAuth firebaseAuth;
  private final UserRepository userRepository;

  public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth, UserRepository userRepository) {
    this.firebaseAuth = firebaseAuth;
    this.userRepository = userRepository;
  }

  private String getBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String idToken = getBearerToken(request);
    if (idToken != null) {
      try {
        FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);
        FirebasePrincipal principal =
            new FirebasePrincipal(decoded.getUid(), decoded.getEmail(), decoded.isEmailVerified());

        Optional<User> localUser = userRepository.findByFirebaseUid(principal.uid());
        List<GrantedAuthority> authorities =
            localUser
                .map(
                    u ->
                        List.<GrantedAuthority>of(
                            new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                .orElse(List.of());

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (FirebaseAuthException ex) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }
}
