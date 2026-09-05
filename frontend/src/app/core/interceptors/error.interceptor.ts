import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { throwError, Observable } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TokenStorageService } from '../services/token-storage.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);
  const tokenStorage = inject(TokenStorageService);

  // Endpoints reachable without a session. A 401 from one of these is a credential failure, so the
  // session-expired handling must not run.
  const PUBLIC_AUTH_PATHS = [
    '/api/auth/login',
    '/api/auth/signup',
    '/api/auth/forgot-password',
    '/api/auth/verify-reset-token',
    '/api/auth/reset-password',
    '/api/auth/verify-email',
    '/api/auth/resend-verification',
  ];
  const isPublicAuthRequest = PUBLIC_AUTH_PATHS.some((path) => request.url.includes(path));

  const getServerErrorMessage = (error: HttpErrorResponse): string => {
    // Try to extract error message from different response formats
    if (error.error?.message) {
      return error.error.message;
    }

    if (error.error?.error) {
      return typeof error.error.error === 'string'
        ? error.error.error
        : JSON.stringify(error.error.error);
    }

    // Default messages based on status code
    switch (error.status) {
      case 0:
        return 'Unable to connect to server. Please check your internet connection.';
      case 400:
        return 'Invalid request. Please check your input.';
      case 401:
        return 'Your session has expired. Please log in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found.';
      case 409:
        return 'This operation conflicts with existing data.';
      case 422:
        return 'Unable to process your request. Please check your input.';
      case 500:
        return 'A server error occurred. Please try again later.';
      case 502:
      case 503:
      case 504:
        return 'The server is temporarily unavailable. Please try again later.';
      default:
        return `An unexpected error occurred (${error.status})`;
    }
  };

  const handleSpecificErrors = (error: HttpErrorResponse): void => {
    switch (error.status) {
      case 401:
        // A 401 from a public auth endpoint means the credentials were wrong, not that a session
        // expired. Treating it as an expiry cleared storage, showed "Your session has expired" and
        // bounced the user to /login?returnUrl=/login on top of the login page's own message.
        if (isPublicAuthRequest) {
          break;
        }
        // Unauthorized - redirect to login
        localStorage.removeItem('currentUser');
        tokenStorage.removeToken();
        router.navigate(['/login'], {
          queryParams: { returnUrl: router.url },
        });
        break;

      case 403:
        // Forbidden - redirect to unauthorized page
        router.navigate(['/unauthorized']);
        break;

      case 404:
        // Not found - could redirect to 404 page
        if (error.url?.includes('/api/')) {
          // API not found - show error message
          console.error('API endpoint not found:', error.url);
        }
        break;

      case 0:
        // Network error - offline
        console.error('Network error - application is offline');
        break;

      default:
        // Log other errors
        console.error('HTTP Error:', error);
        break;
    }
  };

  const showError = (message: string): void => {
    snackBar.open(message, 'Close', {
      duration: 5000,
      horizontalPosition: 'right',
      verticalPosition: 'top',
      panelClass: ['error-snackbar'],
      politeness: 'assertive',
    });
  };

  return next(request).pipe(
    // Retry failed GET requests once, but only for server errors (5xx) and network errors
    retry({
      count: request.method === 'GET' ? 1 : 0,
      delay: (error: HttpErrorResponse) => {
        // Only retry on 5xx or network errors (status 0), not on 4xx client errors
        if (error.status === 0 || error.status >= 500) {
          return new Observable((subscriber) => {
            setTimeout(() => subscriber.next(undefined as any), 1000);
          });
        }
        throw error;
      },
    }),
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${error.error.message}`;
      } else {
        // Server-side error
        errorMessage = getServerErrorMessage(error);
      }

      // Handle specific error codes
      handleSpecificErrors(error);

      // Display error message to user (except for silent errors). A rejected sign-in is reported
      // by the login page itself, so the interceptor stays quiet and the user sees one message.
      const credentialFailure = isPublicAuthRequest && error.status === 401;
      if (!request.headers.has('X-Silent-Error') && !credentialFailure) {
        showError(errorMessage);
      }

      return throwError(() => error);
    })
  );
};
