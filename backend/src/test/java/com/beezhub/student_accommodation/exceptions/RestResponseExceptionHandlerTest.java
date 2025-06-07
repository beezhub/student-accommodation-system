package com.beezhub.student_accommodation.exceptions;

import com.beezhub.student_accommodation.model.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class RestResponseExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private RestResponseExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webRequest.getDescription(false)).thenReturn("test-uri");
    }

    @Test
    void handleEmailAlreadyRegistered_ShouldReturnConflict() {
        // Arrange
        EmailAlreadyRegisteredException ex = new EmailAlreadyRegisteredException("Email already registered");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailAlreadyRegistered(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals("Email already registered", response.getBody().getErrors().get(0));
        assertEquals("test-uri", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleUserNotFound_ShouldReturnNotFound() {
        // Arrange
        UserNotFoundException ex = new UserNotFoundException("User not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserNotFound(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertEquals("User not found", response.getBody().getErrors().get(0));
        assertEquals("test-uri", response.getBody().getPath());
    }

    @Test
    void handleBadCredentials_ShouldReturnUnauthorized() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, webRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
        assertEquals("Invalid email or password", response.getBody().getErrors().getFirst());
        assertEquals("test-uri", response.getBody().getPath());
    }
}