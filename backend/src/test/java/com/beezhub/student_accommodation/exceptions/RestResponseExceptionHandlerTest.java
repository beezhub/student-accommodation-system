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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void handleRuntimeException() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntimeException(new RuntimeException("Runtime error"), webRequest);
        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Runtime error");
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
    
    @Test
    void handleFileStorageException() {
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleFileStorageException(new FileStorageException("Storage failed"), webRequest);
        assertErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE, "Storage failed");
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
}