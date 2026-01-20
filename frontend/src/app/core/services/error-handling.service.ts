import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

export type ErrorType = 'network' | 'server' | 'validation' | 'notfound' | 'unauthorized' | 'forbidden' | 'conflict' | 'general';

export interface ParsedError {
  type: ErrorType;
  title: string;
  message: string;
  details?: string[];
  canRetry: boolean;
  statusCode?: number;
}

export interface ErrorDisplayOptions {
  showSnackbar?: boolean;
  snackbarDuration?: number;
  navigateOnUnauthorized?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ErrorHandlingService {
  private defaultOptions: ErrorDisplayOptions = {
    showSnackbar: true,
    snackbarDuration: 5000,
    navigateOnUnauthorized: true,
  };

  constructor(
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  /**
   * Parse an HTTP error response into a standardized format
   */
  parseError(error: HttpErrorResponse | Error | unknown): ParsedError {
    if (error instanceof HttpErrorResponse) {
      return this.parseHttpError(error);
    }

    if (error instanceof Error) {
      return {
        type: 'general',
        title: 'Error',
        message: error.message || 'An unexpected error occurred.',
        canRetry: true,
      };
    }

    return {
      type: 'general',
      title: 'Error',
      message: 'An unexpected error occurred.',
      canRetry: true,
    };
  }

  /**
   * Parse HTTP error responses
   */
  private parseHttpError(error: HttpErrorResponse): ParsedError {
    const statusCode = error.status;

    // Network error (no response from server)
    if (statusCode === 0) {
      return {
        type: 'network',
        title: 'Connection Error',
        message: 'Unable to connect to the server. Please check your internet connection.',
        canRetry: true,
        statusCode: 0,
      };
    }

    // Extract message from error response
    const backendMessage = this.extractBackendMessage(error);

    switch (statusCode) {
      case 400:
        return {
          type: 'validation',
          title: 'Invalid Request',
          message: backendMessage || 'The request contains invalid data. Please check your input.',
          details: this.extractValidationErrors(error),
          canRetry: false,
          statusCode,
        };

      case 401:
        return {
          type: 'unauthorized',
          title: 'Session Expired',
          message: backendMessage || 'Your session has expired. Please log in again.',
          canRetry: false,
          statusCode,
        };

      case 403:
        return {
          type: 'forbidden',
          title: 'Access Denied',
          message: backendMessage || 'You do not have permission to perform this action.',
          canRetry: false,
          statusCode,
        };

      case 404:
        return {
          type: 'notfound',
          title: 'Not Found',
          message: backendMessage || 'The requested resource could not be found.',
          canRetry: false,
          statusCode,
        };

      case 409:
        return {
          type: 'conflict',
          title: 'Conflict',
          message: backendMessage || 'This action conflicts with an existing resource.',
          canRetry: false,
          statusCode,
        };

      case 422:
        return {
          type: 'validation',
          title: 'Validation Error',
          message: backendMessage || 'The submitted data failed validation.',
          details: this.extractValidationErrors(error),
          canRetry: false,
          statusCode,
        };

      case 500:
      case 502:
      case 503:
      case 504:
        return {
          type: 'server',
          title: 'Server Error',
          message: 'A server error occurred. Please try again later.',
          canRetry: true,
          statusCode,
        };

      default:
        return {
          type: 'general',
          title: 'Error',
          message: backendMessage || 'An unexpected error occurred. Please try again.',
          canRetry: true,
          statusCode,
        };
    }
  }

  /**
   * Extract error message from backend response
   */
  private extractBackendMessage(error: HttpErrorResponse): string | null {
    const body = error.error;

    if (typeof body === 'string') {
      return body;
    }

    if (body && typeof body === 'object') {
      // Common backend error response formats
      return body.message || body.error || body.detail || body.title || null;
    }

    return null;
  }

  /**
   * Extract validation errors from backend response
   */
  private extractValidationErrors(error: HttpErrorResponse): string[] | undefined {
    const body = error.error;

    if (!body || typeof body !== 'object') {
      return undefined;
    }

    const errors: string[] = [];

    // Handle different validation error formats
    if (body.errors && Array.isArray(body.errors)) {
      errors.push(...body.errors);
    }

    if (body.fieldErrors && typeof body.fieldErrors === 'object') {
      Object.entries(body.fieldErrors).forEach(([field, messages]) => {
        if (Array.isArray(messages)) {
          messages.forEach((msg) => errors.push(`${field}: ${msg}`));
        } else if (typeof messages === 'string') {
          errors.push(`${field}: ${messages}`);
        }
      });
    }

    if (body.violations && Array.isArray(body.violations)) {
      body.violations.forEach((v: { field?: string; message?: string }) => {
        if (v.message) {
          errors.push(v.field ? `${v.field}: ${v.message}` : v.message);
        }
      });
    }

    return errors.length > 0 ? errors : undefined;
  }

  /**
   * Handle error and optionally show snackbar
   */
  handleError(
    error: HttpErrorResponse | Error | unknown,
    options?: ErrorDisplayOptions
  ): ParsedError {
    const opts = { ...this.defaultOptions, ...options };
    const parsed = this.parseError(error);

    if (opts.showSnackbar) {
      this.showErrorSnackbar(parsed, opts.snackbarDuration);
    }

    if (opts.navigateOnUnauthorized && parsed.type === 'unauthorized') {
      this.router.navigate(['/login']);
    }

    return parsed;
  }

  /**
   * Show error message in snackbar
   */
  showErrorSnackbar(error: ParsedError | string, duration = 5000): void {
    const message = typeof error === 'string' ? error : error.message;

    this.snackBar.open(message, 'Close', {
      duration,
      panelClass: ['error-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }

  /**
   * Show success message in snackbar
   */
  showSuccess(message: string, duration = 3000): void {
    this.snackBar.open(message, 'Close', {
      duration,
      panelClass: ['success-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }

  /**
   * Show warning message in snackbar
   */
  showWarning(message: string, duration = 4000): void {
    this.snackBar.open(message, 'Close', {
      duration,
      panelClass: ['warning-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }

  /**
   * Show info message in snackbar
   */
  showInfo(message: string, duration = 3000): void {
    this.snackBar.open(message, 'Close', {
      duration,
      panelClass: ['info-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }

  /**
   * Get user-friendly message for common errors
   */
  getActionErrorMessage(action: string, error: ParsedError): string {
    switch (error.type) {
      case 'network':
        return `Unable to ${action}. Please check your internet connection.`;
      case 'unauthorized':
        return `Unable to ${action}. Please log in again.`;
      case 'forbidden':
        return `You don't have permission to ${action}.`;
      case 'notfound':
        return `Unable to ${action}. The resource was not found.`;
      case 'validation':
        return error.message;
      case 'server':
        return `Unable to ${action}. Server error occurred. Please try again later.`;
      default:
        return error.message || `Unable to ${action}. Please try again.`;
    }
  }
}
