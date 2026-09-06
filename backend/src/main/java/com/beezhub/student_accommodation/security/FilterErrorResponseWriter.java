package com.beezhub.student_accommodation.security;

import com.beezhub.student_accommodation.model.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Writes an {@link ErrorResponse} body from inside the servlet filter chain.
 *
 * <p>Filters run before the {@code DispatcherServlet}, so {@code @ControllerAdvice} does not see
 * what they throw. {@code JwtAuthenticationFilter} used to hand its exceptions to the shared
 * {@code HandlerExceptionResolver} with a null handler, but {@code RestResponseExceptionHandler} is
 * declared with {@code basePackages}, and {@code ControllerAdviceBean.isApplicableToBeanType(null)}
 * is false for any advice carrying selectors. Every filter-chain exception therefore skipped the
 * global handler and fell through to the container error page, whose body shape the frontend cannot
 * read (GH-204).
 *
 * <p>Writing the body here rather than dropping the {@code basePackages} selector keeps the change
 * to the filters that need it: widening the advice would also have applied it to springdoc, FF4j
 * and actuator endpoints, which is a much larger behavioural change than the bug warrants.
 *
 * <p>The status and message rules mirror {@code RestResponseExceptionHandler}: an authentication
 * failure is a 401 with a fixed message, and anything else is a 500 carrying a reference that
 * correlates with the ERROR log line holding the detail. Exception text never reaches the client,
 * because at this point in the chain the caller is by definition unauthenticated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FilterErrorResponseWriter {

  /** Deliberately says nothing about which check failed, or whether the account exists. */
  private static final String AUTHENTICATION_FAILED = "Authentication failed. Please log in again.";

  private final ObjectMapper objectMapper;

  /**
   * Maps {@code exception} to a status and writes the body. Authentication failures become 401,
   * everything else 500.
   */
  public void write(HttpServletRequest request, HttpServletResponse response, Exception exception)
      throws IOException {
    if (isAuthenticationFailure(exception)) {
      log.warn(
          "action=authenticationFailed type={} method={} path={}",
          exception.getClass().getName(),
          request.getMethod(),
          request.getRequestURI());
      write(request, response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_FAILED);
      return;
    }

    String reference = reference();
    log.error(
        "action=filterChainException reference={} type={} method={} path={}",
        reference,
        exception.getClass().getName(),
        request.getMethod(),
        request.getRequestURI(),
        exception);
    write(
        request,
        response,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Reference: " + reference);
  }

  /** Writes an {@link ErrorResponse} with an explicit status and message. */
  public void write(
      HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
      throws IOException {
    if (response.isCommitted()) {
      // Something downstream already started writing; overwriting it would corrupt that response.
      log.warn(
          "action=errorResponseSkipped reason=responseCommitted method={} path={}",
          request.getMethod(),
          request.getRequestURI());
      return;
    }

    response.resetBuffer();
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(
        response.getOutputStream(),
        new ErrorResponse(LocalDateTime.now(), status.value(), message, request.getRequestURI()));
    response.flushBuffer();
  }

  private static boolean isAuthenticationFailure(Exception exception) {
    return exception instanceof AuthenticationException || exception instanceof JwtException;
  }

  /**
   * Correlates the generic body with the ERROR log line holding the detail. Not a secret:
   * RequestLoggingFilter already returns the same id in the X-Request-ID response header.
   */
  private static String reference() {
    String requestId = MDC.get("requestId");
    return (requestId == null || requestId.isBlank()) ? "unavailable" : requestId;
  }
}
