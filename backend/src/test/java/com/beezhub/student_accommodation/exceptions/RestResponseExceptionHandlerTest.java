package com.beezhub.student_accommodation.exceptions;

import com.beezhub.student_accommodation.model.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.MDC;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
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

    private void assertErrorResponse(ResponseEntity<?> response, HttpStatus expectedStatus, String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        if (response.getBody() instanceof ErrorResponse) {
            ErrorResponse errorResponse = (ErrorResponse) response.getBody();
            assertEquals(expectedStatus.value(), errorResponse.getStatus());
            assertEquals(expectedMessage, errorResponse.getErrors().get(0));
            assertEquals("test-uri", errorResponse.getPath());
            assertNotNull(errorResponse.getTimestamp());
        }
    }

    @Test
    void handleConflict() {
        ResponseEntity<Object> response = exceptionHandler.handleConflict(new IllegalArgumentException("Illegal argument"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Illegal argument");
    }

    /**
     * The inverse of what this asserted before. GH-149 had to treat java.lang.RuntimeException as
     * carrying a client-safe message, because 53 sites in main threw it with user-facing text.
     * Those sites now throw domain or infrastructure exceptions (GH-205), so a bare RuntimeException
     * is once again what it looks like: something unplanned, whose text is not ours and must not be
     * echoed.
     */
    @Test
    void handleRuntimeException_bareRuntimeException_isNoLongerTreatedAsClientSafe() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(
                new RuntimeException("connection string jdbc:postgresql://db:5432 with password hunter2"),
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertTrue(message.startsWith("An unexpected error occurred"));
        assertFalse(message.contains("hunter2"));
    }

    /** A domain exception without its own handler also keeps its message. */
    @Test
    void handleRuntimeException_domainException_keepsMessageAndStatus() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(
                new ResourceNotFoundException("Building not found"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Building not found");
    }

    /** The prepared statement and the column name must not reach the client. */
    @Test
    void handleRuntimeException_frameworkException_returnsGenericServerError() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(
                new InvalidDataAccessResourceUsageException(
                        "could not prepare statement [ERROR: column \"foo\" does not exist] "
                                + "[select r1_0.id from room r1_0 where r1_0.foo = ?]"),
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertTrue(message.startsWith("An unexpected error occurred"));
        assertFalse(message.contains("select"));
        assertFalse(message.contains("column"));
    }

    /** The entity class name and its primary key must not reach the client. */
    @Test
    void handleRuntimeException_lazyInitialization_hidesEntityDetail() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(
                new org.hibernate.LazyInitializationException(
                        "could not initialize proxy [com.beezhub.student_accommodation.model.entity.Student#42]"
                                + " - no Session"),
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().getErrors().get(0).contains("Student#42"));
    }

    /** The reference is what connects the generic body to the log line holding the detail. */
    @Test
    void handleRuntimeException_frameworkException_includesRequestId() {
        MDC.put("requestId", "req-123");
        try {
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(
                    new InvalidDataAccessResourceUsageException("internal detail"), webRequest);
            assertTrue(response.getBody().getErrors().get(0).contains("req-123"));
        } finally {
            MDC.remove("requestId");
        }
    }

    /**
     * A RuntimeException with no message used to make List.of throw inside the handler, so the
     * request fell through to the container error page. The guard now lives in ErrorResponse, which
     * every handler builds its body through.
     *
     * <p>The status is 500 rather than the 400 it was: a bare RuntimeException is no longer treated
     * as carrying a client-safe message (GH-205). What this test is about — that a null message does
     * not blow up the handler itself — is unchanged.
     */
    @Test
    void handleRuntimeException_nullMessage_doesNotThrow() {
        ResponseEntity<ErrorResponse> response = assertDoesNotThrow(
                () -> exceptionHandler.handleRuntimeException(new RuntimeException(), webRequest));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().getErrors().get(0));
    }

    /** A domain exception with no message must not blow up the handler either. */
    @Test
    void handleRuntimeException_domainExceptionWithNullMessage_doesNotThrow() {
        ResponseEntity<ErrorResponse> response = assertDoesNotThrow(
                () -> exceptionHandler.handleRuntimeException(
                        new ResourceNotFoundException(null), webRequest));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getErrors().get(0));
    }

    @Test
    void handleConflict_nullMessage_doesNotThrow() {
        ResponseEntity<Object> response = assertDoesNotThrow(
                () -> exceptionHandler.handleConflict(new IllegalStateException(), webRequest));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    /**
     * Without its own handler this would be swallowed by the catch-all and returned as an opaque
     * 500, losing the caller's ability to tell a retryable write conflict from a server fault.
     */
    @Test
    void handleOptimisticLocking_returnsConflictWithoutEntityDetail() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException(
                        com.beezhub.student_accommodation.model.entity.Document.class, 42L),
                webRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertFalse(message.contains("Document"));
        assertFalse(message.contains("42"));
    }

    /** Raised inside the authentication manager, so its message carries JPA and connection detail. */
    @Test
    void handleInternalAuthenticationService_returnsGenericServerError() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInternalAuthenticationService(
                new InternalAuthenticationServiceException(
                        "Could not open JPA EntityManager for transaction; jdbc:postgresql://db:5432/sas"),
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertFalse(message.contains("jdbc:postgresql"));
        assertFalse(message.contains("EntityManager"));
    }

    @Test
    void handleInvalidStatus_returnsBadRequestWithMessage() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidStatus(
                new InvalidStatusException("Invalid application status: BOGUS"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid application status: BOGUS");
    }

    @Test
    void handleEmailAlreadyRegistered_ShouldReturnConflict() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailAlreadyRegistered(new EmailAlreadyRegisteredException("Email already registered"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Email already registered");
    }

    @Test
    void handleUserNotFound_ShouldReturnNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserNotFound(new UserNotFoundException("User not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "User not found");
    }
    
    @Test
    void handleStudentNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleStudentNotFound(new StudentNotFoundException("Student not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Student not found");
    }
    
    @Test
    void handleApplicationNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleApplicationNotFound(new ApplicationNotFoundException("Application not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Application not found");
    }
    
    @Test
    void handleInstitutionNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInstitutionNotFound(new InstitutionNotFoundException("Institution not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Institution not found");
    }
    
    @Test
    void handleAcademicYearNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAcademicYearNotFound(new AcademicYearNotFoundException("Academic Year not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Academic Year not found");
    }
    
    @Test
    void handleApplicationWindowClosed() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleApplicationWindowClosed(new ApplicationWindowClosedException("Window closed"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Window closed");
    }
    
    @Test
    void handleDocumentTypeNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDocumentTypeNotFound(new DocumentTypeNotFoundException("Document type not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Document type not found");
    }

    @Test
    void handleYearOfStudyNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleYearOfStudyNotFound(new YearOfStudyNotFoundException("Year of study not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Year of study not found");
    }
    
    @Test
    void handleBuildingNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBuildingNotFound(new BuildingNotFoundException("Building not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Building not found");
    }
    
    @Test
    void handleRoomNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRoomNotFound(new RoomNotFoundException("Room not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Room not found");
    }
    
    @Test
    void handleRoomAllocationNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRoomAllocationNotFound(new RoomAllocationNotFoundException("Allocation not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Allocation not found");
    }
    
    @Test
    void handleRoomCapacityExceeded() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRoomCapacityExceeded(new RoomCapacityExceededException("Capacity exceeded"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Capacity exceeded");
    }
    
    @Test
    void handleStudentAlreadyAllocated() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleStudentAlreadyAllocated(new StudentAlreadyAllocatedException("Already allocated"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Already allocated");
    }
    
    @Test
    void handleInvalidAllocationStatusTransition() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidAllocationStatusTransition(new InvalidAllocationStatusTransitionException("Invalid transition"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Invalid transition");
    }
    
    @Test
    void handleApplicationNotApproved() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleApplicationNotApproved(new ApplicationNotApprovedException("Not approved"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Not approved");
    }

    @Test
    void handleApplicationAlreadyAllocated() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleApplicationAlreadyAllocated(new ApplicationAlreadyAllocatedException("Application already allocated"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Application already allocated");
    }
    
    @Test
    void handleDuplicateApplication() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateApplication(new DuplicateApplicationException("Duplicate application"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Duplicate application");
    }
    
    @Test
    void handleResourceNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(new ResourceNotFoundException("Resource not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Resource not found");
    }
    
    @Test
    void handleDuplicateResource() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateResource(new DuplicateResourceException("Duplicate resource"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Duplicate resource");
    }

    @Test
    void handleUnauthorizedAccess() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedAccess(new UnauthorizedAccessException("Unauthorized"), webRequest);
        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Unauthorized");
    }

    @Test
    void handleBadCredentials_ShouldReturnUnauthorized() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(new BadCredentialsException("Bad credentials"), webRequest);
        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    
    @Test
    void handleAuthenticationException() {
        AuthenticationException ex = new AuthenticationException("Auth failed") {};
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex, webRequest);
        assertErrorResponse(response, HttpStatus.UNAUTHORIZED, "Auth failed");
    }
    
    @Test
    void handleInvalidToken() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidToken(new InvalidTokenException("Invalid token"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid token");
    }
    
    @Test
    void handleTokenExpired() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTokenExpired(new TokenExpiredException("Token expired"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Token expired");
    }
    
    @Test
    void handleEmailAlreadyVerified() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailAlreadyVerified(new EmailAlreadyVerifiedException("Email verified"), webRequest);
        assertErrorResponse(response, HttpStatus.CONFLICT, "Email verified");
    }
    
    @Test
    void handlePasswordChangeRequired() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePasswordChangeRequired(new PasswordChangeRequiredException("Change password"), webRequest);
        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Change password");
    }

    @Test
    void handleEmailNotVerified() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailNotVerified(new EmailNotVerifiedException("Email not verified"), webRequest);
        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Email not verified");
    }

    @Test
    void handleAccountLocked() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccountLocked(new AccountLockedException("Account locked"), webRequest);
        assertErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS, "Account locked");
    }

    @Test
    void handleTenantNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTenantNotFound(new TenantNotFoundException("Tenant not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Tenant not found");
    }

    @Test
    void handleDocumentCommentNotFound() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDocumentCommentNotFound(new DocumentCommentNotFoundException("Comment not found"), webRequest);
        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Comment not found");
    }

    @Test
    void handleFileValidationException() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileValidationException(new FileValidationException("Validation failed"), webRequest);
        assertErrorResponse(response, HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed");
    }
    
    /**
     * Still 503, but the message is no longer echoed: these carry the object key and MinIO's driver
     * text, which the caller cannot act on (GH-205). The detail-hiding is asserted separately in
     * handleFileStorage_isServiceUnavailableAndHidesTheObjectKey.
     */
    @Test
    void handleFileStorageException() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(new FileStorageException("Storage failed"), webRequest);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().getErrors().get(0).contains("temporarily unavailable"));
    }

    @Test
    void handleMethodArgumentNotValid() throws NoSuchMethodException {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("objectName", "field", "Invalid value")));
        
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        
        ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentNotValid(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
        assertEquals("Invalid value", errorResponse.getErrors().get(0));
    }

    // ── GH-205: infrastructure failures are 503, and say nothing about the infrastructure ─────

    /**
     * An SMTP outage used to answer 400 with the mail driver's own text, because the site threw a
     * bare RuntimeException and GH-149 had to treat that as client-safe. A client error is also
     * invisible to 5xx alerting, so nobody was told the mail server was down.
     */
    @Test
    void handleExternalService_isServiceUnavailableAndHidesTheDetail() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleExternalService(
                new ExternalServiceException(
                        "Failed to send email",
                        new IllegalStateException("smtp.internal.example:587 auth failed for postmaster")),
                webRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertTrue(message.contains("temporarily unavailable"));
        assertFalse(message.contains("smtp.internal.example"));
        assertFalse(message.contains("postmaster"));
    }

    /** A feature this deployment never configured is still unavailable to the caller. */
    @Test
    void handleServiceNotConfigured_isServiceUnavailable() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceNotConfigured(
                new ServiceNotConfiguredException("Geocoding is not configured"), webRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().getErrors().get(0).contains("not enabled"));
    }

    /** Storage failures carry the object key and the driver's text; neither is the caller's business. */
    @Test
    void handleFileStorage_isServiceUnavailableAndHidesTheObjectKey() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(
                new FileStorageException("Error uploading file to MinIO: student/12/passport-scan.pdf"),
                webRequest);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        String message = response.getBody().getErrors().get(0);
        assertTrue(message.contains("temporarily unavailable"));
        assertFalse(message.contains("passport-scan"));
    }

    /** An allocation rule refusal is the admin's to act on, so its message is theirs to read. */
    @Test
    void handleAllocationConstraint_isConflictAndKeepsTheMessage() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAllocationConstraint(
                new AllocationConstraintViolationException(
                        "New room does not meet hard constraints for student"),
                webRequest);

        assertErrorResponse(response, HttpStatus.CONFLICT,
                "New room does not meet hard constraints for student");
    }
}
