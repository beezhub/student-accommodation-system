package com.beezhub.student_accommodation.exceptions;

import com.beezhub.student_accommodation.model.dto.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(basePackages = "com.beezhub.student_accommodation.controller")
@Slf4j
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
                        DataIntegrityViolationException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "A record with the same unique value already exists",
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
        protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, new HttpHeaders(), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.FORBIDDEN.value(),
                                "Access denied: you do not have permission to perform this action",
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        /**
         * Catch-all for runtime exceptions with no more specific handler.
         *
         * <p>This used to copy {@code ex.getMessage()} into the body and return 400 for anything at
         * all, so Hibernate and Spring Data messages reached the client intact: prepared statements
         * with the full SQL, entity class names with primary keys, and JDK helpful-NPE text naming
         * fields. The hardcoded 400 also meant genuine server faults never showed up as 5xx in the
         * metrics, and the frontend interceptor never retried a transient database blip (GH-149).
         *
         * <p>Discrimination is by type rather than blanket genericisation. Messages this codebase
         * wrote are user-facing and keep their old behaviour exactly; framework and JDK types get a
         * fixed message, a 500 and an ERROR log carrying the detail.
         */
        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
                if (carriesClientSafeMessage(ex)) {
                        log.warn("action=businessRuleViolation type={} path={}",
                                        ex.getClass().getName(), request.getDescription(false));
                        return new ResponseEntity<>(new ErrorResponse(
                                        LocalDateTime.now(),
                                        HttpStatus.BAD_REQUEST.value(),
                                        ex.getMessage(),
                                        request.getDescription(false)), HttpStatus.BAD_REQUEST);
                }
                String reference = reference();
                log.error("action=unhandledException reference={} type={} path={}",
                                reference, ex.getClass().getName(), request.getDescription(false), ex);
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "An unexpected error occurred. Reference: " + reference,
                                request.getDescription(false)), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /**
         * Without this, an optimistic-lock conflict would be swallowed by the catch-all above and
         * returned as an opaque 500. Room, Application and Document all carry a version column, and
         * DocumentStatusService rethrows this deliberately so the caller can retry. 409 says that.
         */
        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> handleOptimisticLocking(
                        OptimisticLockingFailureException ex, WebRequest request) {
                log.warn("action=optimisticLockConflict path={}", request.getDescription(false));
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                "This record was changed by someone else. Reload and try again.",
                                request.getDescription(false)), HttpStatus.CONFLICT);
        }

        /**
         * Raised inside the authentication manager when the failure is infrastructure rather than a
         * bad credential, so its message carries JPA and connection detail. The general
         * AuthenticationException handler below echoes messages, which handed that detail to an
         * unauthenticated caller of the login endpoint.
         */
        @ExceptionHandler(InternalAuthenticationServiceException.class)
        public ResponseEntity<ErrorResponse> handleInternalAuthenticationService(
                        InternalAuthenticationServiceException ex, WebRequest request) {
                String reference = reference();
                log.error("action=authenticationInfrastructureFailure reference={} path={}",
                                reference, request.getDescription(false), ex);
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Authentication is temporarily unavailable. Reference: " + reference,
                                request.getDescription(false)), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        /**
         * Thrown where a request string failed to map onto a status enum. Without its own handler it
         * fell to the catch-all, which is now a 500, and a rejected input is the caller to fix.
         */
        /**
         * A student tried to replace or delete a document an administrator already approved while
         * the application it supports is still live. 409 rather than 403: the caller owns the
         * document, the state of the review is what refuses (GH-145).
         */
        @ExceptionHandler(DocumentLockedException.class)
        public ResponseEntity<ErrorResponse> handleDocumentLocked(DocumentLockedException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.CONFLICT);
        }

        /** The request's value is wrong whatever the state of the building, so 400. */
        @ExceptionHandler(InvalidAccessibilityFeaturesException.class)
        public ResponseEntity<ErrorResponse> handleInvalidAccessibilityFeatures(
                        InvalidAccessibilityFeaturesException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.BAD_REQUEST);
        }

        /**
         * An admin account with no company cannot be unlocked: that would open every company's data
         * to it (#412). 409 rather than 403: the caller may unlock accounts, the account's state is
         * what refuses, and the message names the way forward.
         */
        @ExceptionHandler(UnassignedAdminUnlockException.class)
        public ResponseEntity<ErrorResponse> handleUnassignedAdminUnlock(
                        UnassignedAdminUnlockException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.CONFLICT);
        }

        /**
         * A delete refused because students still hold a place in the room or building. The message
         * tells the admin what to do first, so it reaches them unchanged.
         */
        @ExceptionHandler(ResourceInUseException.class)
        public ResponseEntity<ErrorResponse> handleResourceInUse(
                        ResourceInUseException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.CONFLICT);
        }

        @ExceptionHandler(InvalidStatusException.class)
        public ResponseEntity<ErrorResponse> handleInvalidStatus(InvalidStatusException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.BAD_REQUEST);
        }

        /**
         * 400, not the 409 the one pre-existing inactive-tenant guard produced by reusing
         * {@link DuplicateResourceException}. Nothing is duplicated: the request names a company
         * that is not accepting new records (GH-267).
         */
        @ExceptionHandler(InactiveTenantException.class)
        public ResponseEntity<ErrorResponse> handleInactiveTenant(InactiveTenantException ex, WebRequest request) {
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.BAD_REQUEST);
        }

        /**
         * True when the message was written by this codebase and is meant for a user.
         *
         * <p>Exactly {@code java.lang.RuntimeException} means a deliberate throw from this project.
         * That is a pragmatic allowance for call sites predating the house rule against generic
         * runtime exceptions, not an endorsement of it. The clean end state migrates them to domain
         * exceptions and deletes the clause.
         */
        private static boolean carriesClientSafeMessage(RuntimeException ex) {
                // java.lang.RuntimeException itself used to be listed here, because 53 sites threw
                // it with user-facing text that admin dialogs render directly. Those sites now
                // throw domain or infrastructure exceptions (GH-205), so the allowance is gone —
                // and with it the risk that a third-party library throwing a bare RuntimeException
                // with sensitive text has that text echoed to a client.
                return ex.getClass().getName().startsWith("com.beezhub.student_accommodation.");
        }

        /**
         * Correlates the generic body with the ERROR log line holding the detail. Not a secret:
         * RequestLoggingFilter already returns the same id in the X-Request-ID response header.
         */
        private static String reference() {
                String requestId = MDC.get("requestId");
                return (requestId == null || requestId.isBlank()) ? "unavailable" : requestId;
        }

        @ExceptionHandler(EmailAlreadyRegisteredException.class)
        public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(
                        EmailAlreadyRegisteredException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(
                        UserNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(StudentNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleStudentNotFound(
                        StudentNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(ApplicationNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleApplicationNotFound(
                        ApplicationNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InstitutionNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleInstitutionNotFound(
                        InstitutionNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(AcademicYearNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleAcademicYearNotFound(
                        AcademicYearNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(ApplicationWindowClosedException.class)
        public ResponseEntity<ErrorResponse> handleApplicationWindowClosed(
                        ApplicationWindowClosedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(DocumentTypeNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleDocumentTypeNotFound(
                        DocumentTypeNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(YearOfStudyNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleYearOfStudyNotFound(
                        YearOfStudyNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(BuildingNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleBuildingNotFound(
                        BuildingNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RoomNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleRoomNotFound(
                        RoomNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RoomAllocationNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleRoomAllocationNotFound(
                        RoomAllocationNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(RoomCapacityExceededException.class)
        public ResponseEntity<ErrorResponse> handleRoomCapacityExceeded(
                        RoomCapacityExceededException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(StudentAlreadyAllocatedException.class)
        public ResponseEntity<ErrorResponse> handleStudentAlreadyAllocated(
                        StudentAlreadyAllocatedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(InvalidAllocationStatusTransitionException.class)
        public ResponseEntity<ErrorResponse> handleInvalidAllocationStatusTransition(
                        InvalidAllocationStatusTransitionException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(ApplicationNotApprovedException.class)
        public ResponseEntity<ErrorResponse> handleApplicationNotApproved(
                        ApplicationNotApprovedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ApplicationAlreadyAllocatedException.class)
        public ResponseEntity<ErrorResponse> handleApplicationAlreadyAllocated(
                        ApplicationAlreadyAllocatedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(DuplicateApplicationException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateApplication(
                        DuplicateApplicationException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateResource(
                        DuplicateResourceException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(UnauthorizedAccessException.class)
        public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
                        UnauthorizedAccessException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.FORBIDDEN.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(
                        BadCredentialsException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                "Invalid email or password",
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        AuthenticationException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(InvalidTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidToken(
                        InvalidTokenException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(TokenExpiredException.class)
        public ResponseEntity<ErrorResponse> handleTokenExpired(
                        TokenExpiredException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.BAD_REQUEST.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(EmailAlreadyVerifiedException.class)
        public ResponseEntity<ErrorResponse> handleEmailAlreadyVerified(
                        EmailAlreadyVerifiedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(PasswordChangeRequiredException.class)
        public ResponseEntity<ErrorResponse> handlePasswordChangeRequired(
                        PasswordChangeRequiredException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.FORBIDDEN.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(EmailNotVerifiedException.class)
        public ResponseEntity<ErrorResponse> handleEmailNotVerified(
                        EmailNotVerifiedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.FORBIDDEN.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(AccountLockedException.class)
        public ResponseEntity<ErrorResponse> handleAccountLocked(
                        AccountLockedException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
        }

        @ExceptionHandler(TenantNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTenantNotFound(
                        TenantNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(DocumentCommentNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleDocumentCommentNotFound(
                        DocumentCommentNotFoundException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.NOT_FOUND.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(FileValidationException.class)
        public ResponseEntity<ErrorResponse> handleFileValidationException(
                        FileValidationException ex, WebRequest request) {
                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                                ex.getMessage(),
                                request.getDescription(false));
                return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        /**
         * Object storage failed. 503 rather than 500: the caller did nothing wrong and the request
         * may succeed later.
         *
         * <p>The message is no longer echoed. These carry the object key and the storage SDK's own
         * text, which is internal detail the caller cannot act on (GH-205).
         */
        @ExceptionHandler(FileStorageException.class)
        public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex, WebRequest request) {
                String reference = reference();
                log.error("action=fileStorageFailure reference={} path={}",
                                reference, request.getDescription(false), ex);
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "File storage is temporarily unavailable. Reference: " + reference,
                                request.getDescription(false)), HttpStatus.SERVICE_UNAVAILABLE);
        }

        /**
         * A service this application depends on failed: mail, report generation, or similar.
         *
         * <p>These were bare RuntimeExceptions, which GH-149 had to treat as carrying a client-safe
         * message, so an SMTP outage answered 400 with the driver's text and never showed up in 5xx
         * alerting. 503 with a fixed sentence and a reference, detail in the log (GH-205).
         */
        @ExceptionHandler(ExternalServiceException.class)
        public ResponseEntity<ErrorResponse> handleExternalService(
                        ExternalServiceException ex, WebRequest request) {
                String reference = reference();
                log.error("action=externalServiceFailure reference={} path={}",
                                reference, request.getDescription(false), ex);
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "A service this action depends on is temporarily unavailable. Reference: "
                                                + reference,
                                request.getDescription(false)), HttpStatus.SERVICE_UNAVAILABLE);
        }

        /**
         * An optional integration this deployment has not configured. The same 503 as an outage,
         * but logged at INFO with no stack trace: nothing failed, and the startup log already says
         * which setting is missing.
         */
        @ExceptionHandler(ServiceNotConfiguredException.class)
        public ResponseEntity<ErrorResponse> handleServiceNotConfigured(
                        ServiceNotConfiguredException ex, WebRequest request) {
                log.info("action=serviceNotConfigured path={} reason={}",
                                request.getDescription(false), ex.getMessage());
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "This feature is not enabled on this deployment.",
                                request.getDescription(false)), HttpStatus.SERVICE_UNAVAILABLE);
        }

        /**
         * An allocation refused by a rule about rooms and students rather than by a status
         * transition. The message is written for the administrator who triggered it.
         */
        @ExceptionHandler(AllocationConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleAllocationConstraint(
                        AllocationConstraintViolationException ex, WebRequest request) {
                log.warn("action=allocationConstraintViolation path={}", request.getDescription(false));
                return new ResponseEntity<>(new ErrorResponse(
                                LocalDateTime.now(),
                                HttpStatus.CONFLICT.value(),
                                ex.getMessage(),
                                request.getDescription(false)), HttpStatus.CONFLICT);
        }

        @Override
        protected ResponseEntity<Object> handleMethodArgumentNotValid(
                        MethodArgumentNotValidException ex,
                        HttpHeaders headers,
                        org.springframework.http.HttpStatusCode status,
                        WebRequest request) {

                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                .collect(Collectors.toList());

                ErrorResponse errorResponse = new ErrorResponse(
                                LocalDateTime.now(),
                                status.value(),
                                errors,
                                request.getDescription(false));

                return new ResponseEntity<>(errorResponse, headers, status);
        }
}