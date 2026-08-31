package com.gryffindor.excalibur.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gryffindor.excalibur.model.response.ErrorResponse;
import com.gryffindor.excalibur.services.RateLimiterService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimiterService rateLimiterService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RateLimitingFilter(RateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    if (path.startsWith("/actuator/")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientKey = extractClientKey(request);
    Bucket bucket;

    if (path.startsWith("/order") && "POST".equalsIgnoreCase(method)) {
      bucket = rateLimiterService.resolveBucket("order:" + clientKey, 10, Duration.ofMinutes(1));
    } else if (path.startsWith("/customer/register") && "POST".equalsIgnoreCase(method)) {
      bucket = rateLimiterService.resolveBucket("register:" + clientKey, 10, Duration.ofMinutes(1));
    } else {
      bucket = rateLimiterService.resolveBucket("public:" + clientKey, 180, Duration.ofMinutes(1));
    }

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
      response.addHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));

      ErrorResponse errorResponse = new ErrorResponse();
      errorResponse.setCode(HttpStatus.TOO_MANY_REQUESTS);
      errorResponse.setMessage(
          "Too many requests. Please try again in " + retryAfterSeconds + " seconds.");
      errorResponse.setRequestId(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY));

      objectMapper.writeValue(response.getWriter(), errorResponse);
    }
  }

  private String extractClientKey(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof FirebasePrincipal firebasePrincipal) {
        return "user:" + firebasePrincipal.uid();
      }
      return "user:" + authentication.getName();
    }

    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return "ip:" + xForwardedFor.split(",")[0].trim();
    }
    return "ip:" + request.getRemoteAddr();
  }
}
