import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { ErrorHandlingService } from './error-handling.service';

/**
 * This service turns every backend failure into the text a user reads, and its 401 branch
 * navigates. Both matter to GH-150 and GH-151, and neither had a test.
 */
describe('ErrorHandlingService', () => {
  let service: ErrorHandlingService;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: jasmine.SpyObj<Router>;

  const httpError = (status: number, body?: unknown) =>
    new HttpErrorResponse({ status, error: body, url: '/api/things' });

  beforeEach(() => {
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/admin/rooms' });

    TestBed.configureTestingModule({
      providers: [
        ErrorHandlingService,
        { provide: MatSnackBar, useValue: snackBar },
        { provide: Router, useValue: router },
      ],
    });
    service = TestBed.inject(ErrorHandlingService);
  });

  // ------------------------------------------------------------- parseError

  it('parseError_networkFailure_isRetryable', () => {
    const parsed = service.parseError(httpError(0));

    expect(parsed.type).toBe('network');
    expect(parsed.canRetry).toBeTrue();
  });

  it('parseError_badRequest_usesTheBackendMessage', () => {
    const parsed = service.parseError(httpError(400, { message: 'Name is required' }));

    expect(parsed.type).toBe('validation');
    expect(parsed.message).toBe('Name is required');
    expect(parsed.canRetry).toBeFalse();
  });

  /** A closed application window is a specific, actionable case rather than a validation error. */
  it('parseError_applicationsClosed_getsItsOwnTitle', () => {
    const parsed = service.parseError(
      httpError(400, { message: 'Applications are not currently open' })
    );

    expect(parsed.title).toBe('Applications Closed');
  });

  it('parseError_unauthorized_isTypedAsSuch', () => {
    expect(service.parseError(httpError(401)).type).toBe('unauthorized');
  });

  /** GH-150 depends on this staying distinct from unauthorized: only 401 may redirect. */
  it('parseError_forbidden_isNotUnauthorized', () => {
    expect(service.parseError(httpError(403)).type).toBe('forbidden');
  });

  it('parseError_notFound_isTypedAsSuch', () => {
    expect(service.parseError(httpError(404)).type).toBe('notfound');
  });

  it('parseError_conflict_isTypedAsSuch', () => {
    expect(service.parseError(httpError(409)).type).toBe('conflict');
  });

  it('parseError_unprocessable_isValidation', () => {
    expect(service.parseError(httpError(422)).type).toBe('validation');
  });

  [500, 502, 503, 504].forEach((status) => {
    it(`parseError_serverError${status}_isRetryable`, () => {
      const parsed = service.parseError(httpError(status));

      expect(parsed.type).toBe('server');
      expect(parsed.canRetry).toBeTrue();
    });
  });

  it('parseError_unmappedStatus_fallsBackToGeneral', () => {
    expect(service.parseError(httpError(418)).type).toBe('general');
  });

  it('parseError_plainError_usesItsMessage', () => {
    const parsed = service.parseError(new Error('boom'));

    expect(parsed.type).toBe('general');
    expect(parsed.message).toBe('boom');
  });

  it('parseError_unknownValue_stillProducesAMessage', () => {
    const parsed = service.parseError('not an error object');

    expect(parsed.type).toBe('general');
    expect(parsed.message).toBeTruthy();
  });

  // ----------------------------------------------- backend message shapes

  it('parseError_stringBody_isUsedDirectly', () => {
    expect(service.parseError(httpError(409, 'Already exists')).message).toBe('Already exists');
  });

  ['message', 'error', 'detail', 'title'].forEach((field) => {
    it(`parseError_bodyWith_${field}_isUsed`, () => {
      const parsed = service.parseError(httpError(409, { [field]: 'From ' + field }));

      expect(parsed.message).toBe('From ' + field);
    });
  });

  it('parseError_bodyWithNoRecognisedField_fallsBackToTheDefault', () => {
    expect(service.parseError(httpError(409, { unexpected: 'x' })).message).toContain('conflicts');
  });

  /** #441: the backend's ErrorResponse only has errors[], so its reason never reached the user. */
  it('parseError_errorResponseBody_usesItsErrorsArray', () => {
    const parsed = service.parseError(
      httpError(409, { status: 409, errors: ['Missing required documents: ID Copy'] })
    );

    expect(parsed.message).toBe('Missing required documents: ID Copy');
  });

  it('parseError_errorsArrayWithNoText_fallsBackToTheDefault', () => {
    expect(service.parseError(httpError(409, { errors: [{ code: 1 }] })).message).toContain(
      'conflicts'
    );
  });

  /** The inline display prints the message and the details, so one reason would print twice. */
  it('parseError_singleError_isNotRepeatedAsADetail', () => {
    const parsed = service.parseError(httpError(400, { errors: ['Name is required'] }));

    expect(parsed.message).toBe('Name is required');
    expect(parsed.details).toBeUndefined();
  });

  /** Unreadable success bodies arrive as { error: SyntaxError, text }, and error is no message. */
  it('parseError_nonStringErrorField_isNotUsedAsTheMessage', () => {
    const parsed = service.parseError(httpError(409, { error: new SyntaxError('bad json') }));

    expect(parsed.message).toContain('conflicts');
  });

  /** nginx answers with an HTML page, which HttpClient hands over as a raw string. */
  it('parseError_htmlBody_isNeverShownToTheUser', () => {
    const page = '<html>\r\n<head><title>404 Not Found</title></head></html>';

    expect(service.parseError(httpError(404, page)).message).not.toContain('<');
    expect(service.parseError(httpError(418, `\n  ${page}`)).message).not.toContain('<');
  });

  it('parseError_payloadTooLarge_saysTheFileIsTooLarge', () => {
    const page = '<html><head><title>413 Request Entity Too Large</title></head></html>';
    const parsed = service.parseError(httpError(413, page));

    expect(parsed.message).toContain('That file is too large');
    expect(parsed.canRetry).toBeFalse();
  });

  // -------------------------------------------------- validation details

  it('parseError_errorsArray_becomesDetails', () => {
    const parsed = service.parseError(httpError(422, { errors: ['Too short', 'Too weak'] }));

    expect(parsed.details).toEqual(['Too short', 'Too weak']);
  });

  it('parseError_fieldErrorsMap_isFlattenedPerField', () => {
    const parsed = service.parseError(
      httpError(422, { fieldErrors: { email: ['is invalid'], name: 'is required' } })
    );

    expect(parsed.details).toEqual(['email: is invalid', 'name: is required']);
  });

  it('parseError_violations_areFlattened', () => {
    const parsed = service.parseError(
      httpError(422, {
        violations: [{ field: 'email', message: 'is invalid' }, { message: 'is bad' }],
      })
    );

    expect(parsed.details).toEqual(['email: is invalid', 'is bad']);
  });

  it('parseError_noValidationDetails_leavesThemUndefined', () => {
    expect(service.parseError(httpError(422, { message: 'nope' })).details).toBeUndefined();
  });

  // -------------------------------------------------------- handleError

  it('handleError_showsASnackbarByDefault', () => {
    service.handleError(httpError(409, { message: 'Already exists' }));

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('handleError_snackbarSuppressed_showsNothing', () => {
    service.handleError(httpError(409), { showSnackbar: false });

    expect(snackBar.open).not.toHaveBeenCalled();
  });

  /** GH-150: this navigation runs after the interceptor has already redirected, so it has to
   *  carry a returnUrl of its own or it strips the one the interceptor set. */
  it('handleError_unauthorized_redirectsToLoginWithAReturnUrl', () => {
    service.handleError(httpError(401));

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/rooms' },
    });
  });

  it('handleError_forbidden_doesNotRedirect', () => {
    service.handleError(httpError(403));

    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('handleError_navigationSuppressed_staysPut', () => {
    service.handleError(httpError(401), { navigateOnUnauthorized: false });

    expect(router.navigate).not.toHaveBeenCalled();
  });

  // ------------------------------------------------------------ messages

  it('showErrorSnackbar_acceptsAPlainString', () => {
    service.showErrorSnackbar('Something went wrong');

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('showErrorSnackbar_acceptsAParsedError', () => {
    service.showErrorSnackbar(service.parseError(httpError(404)));

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('showSuccess_opensASnackbar', () => {
    service.showSuccess('Saved');

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('showWarning_opensASnackbar', () => {
    service.showWarning('Careful');

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('showInfo_opensASnackbar', () => {
    service.showInfo('For your information');

    expect(snackBar.open).toHaveBeenCalled();
  });

  it('getActionErrorMessage_namesTheActionThatFailed', () => {
    const message = service.getActionErrorMessage(
      'save the room',
      service.parseError(httpError(500))
    );

    expect(message).toContain('save the room');
  });
});
