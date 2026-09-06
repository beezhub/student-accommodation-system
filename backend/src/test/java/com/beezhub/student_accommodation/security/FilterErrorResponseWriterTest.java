package com.beezhub.student_accommodation.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * GH-204: filters run before the DispatcherServlet, so nothing they throw reaches
 * {@code @ControllerAdvice}. This writer is what makes a filter-chain failure look like every other
 * error response instead of the container's error page.
 */
class FilterErrorResponseWriterTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final FilterErrorResponseWriter writer = new FilterErrorResponseWriter(objectMapper);

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest("GET", "/api/admin/users");
    request.setRequestURI("/api/admin/users");
    response = new MockHttpServletResponse();
  }

  @Test
  void authenticationFailureBecomesA401InTheErrorResponseShape() throws Exception {
    writer.write(request, response, new BadCredentialsException("Token is blacklisted"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/json");

    JsonNode body = objectMapper.readTree(response.getContentAsString());
    assertThat(body.hasNonNull("timestamp")).isTrue();
    assertThat(body.get("status").asInt()).isEqualTo(401);
    assertThat(body.get("path").asText()).isEqualTo("/api/admin/users");
    assertThat(body.get("errors").isArray()).isTrue();
    assertThat(body.get("errors").get(0).asText()).contains("Authentication failed");
  }

  @Test
  void theReasonForAnAuthenticationFailureNeverReachesTheCaller() throws Exception {
    writer.write(request, response, new BadCredentialsException("Token has been revoked"));

    // "revoked" and "blacklisted" tell an unauthenticated caller which check ran, and by extension
    // that the token was otherwise well-formed and the account real.
    assertThat(response.getContentAsString()).doesNotContain("revoked");
  }

  @Test
  void aMalformedJwtIsAnAuthenticationFailureRatherThanAServerFault() throws Exception {
    writer.write(request, response, new MalformedJwtException("bad token"));

    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void anExpiredJwtIsAnAuthenticationFailureRatherThanAServerFault() throws Exception {
    writer.write(request, response, new ExpiredJwtException(null, null, "expired"));

    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void anythingElseBecomesA500CarryingAReferenceRatherThanTheDetail() throws Exception {
    writer.write(
        request, response, new IllegalStateException("connection pool exhausted at jdbc:postgres"));

    assertThat(response.getStatus()).isEqualTo(500);
    String body = response.getContentAsString();
    assertThat(body).contains("An unexpected error occurred. Reference: ");
    assertThat(body).doesNotContain("jdbc:postgres");
  }

  @Test
  void anExplicitStatusAndMessageAreWrittenAsGiven() throws Exception {
    writer.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests.");

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentAsString()).contains("Too many requests.");
  }

  /**
   * Overwriting a response another filter has already begun writing would corrupt it, so the writer
   * stands down rather than throwing an IllegalStateException from inside the catch block.
   */
  @Test
  void aCommittedResponseIsLeftAlone() throws Exception {
    response.getWriter().write("partial body");
    response.flushBuffer();

    writer.write(request, response, new BadCredentialsException("too late"));

    assertThat(response.getContentAsString()).isEqualTo("partial body");
  }
}
