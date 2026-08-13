package com.gryffindor.excalibur.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a per-request correlation id (reusing an inbound X-Request-Id if the caller already set
 * one), publishes it via MDC so every log line written while handling the request can be tied back
 * to it, echoes it on the response so a client can reference a specific failed request, and logs
 * one summary line per request. Ordered first so the id is in place before Spring Security's filter
 * chain (and thus before {@link FirebaseAuthenticationFilter}) runs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_MDC_KEY = "requestId";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    MDC.put(REQUEST_ID_MDC_KEY, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    long startTimeMs = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTimeMs;
      int status = response.getStatus();
      String summary = "{} {} -> {} ({} ms)";
      Object[] args = {request.getMethod(), request.getRequestURI(), status, durationMs};
      if (status >= 500) {
        log.error(summary, args);
      } else if (status >= 400) {
        log.warn(summary, args);
      } else {
        log.info(summary, args);
      }
      MDC.remove(REQUEST_ID_MDC_KEY);
    }
  }
}
