import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

export type ErrorType =
  | 'network'
  | 'server'
  | 'validation'
  | 'notfound'
  | 'unauthorized'
  | 'forbidden'
  | 'conflict'
  | 'general';

export type ErrorDisplayMode = 'card' | 'inline' | 'banner';

interface ErrorConfig {
  icon: string;
  title: string;
  message: string;
  color: 'warn' | 'accent' | 'primary';
}

@Component({
  selector: 'app-error-display',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatCardModule],
  templateUrl: './error-display.component.html',
  styleUrls: ['./error-display.component.css'],
})
export class ErrorDisplayComponent {
  @Input() type: ErrorType = 'general';
  @Input() mode: ErrorDisplayMode = 'card';
  @Input() title?: string;
  @Input() message?: string;
  @Input() details?: string[];
  @Input() showRetry = true;
  @Input() showGoBack = false;
  @Input() showDismiss = false;
  @Input() customAction?: string;
  @Input() retryLabel = 'Try Again';
  @Input() isRetrying = false;

  @Output() retry = new EventEmitter<void>();
  @Output() goBack = new EventEmitter<void>();
  @Output() dismiss = new EventEmitter<void>();
  @Output() customActionClick = new EventEmitter<void>();

  private configs: Record<ErrorType, ErrorConfig> = {
    network: {
      icon: 'cloud_off',
      title: 'Connection Error',
      message:
        'Unable to connect to the server. Please check your internet connection and try again.',
      color: 'warn',
    },
    server: {
      icon: 'error_outline',
      title: 'Server Error',
      message: 'A server error occurred. Our team has been notified. Please try again later.',
      color: 'warn',
    },
    validation: {
      icon: 'warning',
      title: 'Invalid Input',
      message: 'Please check your input and try again.',
      color: 'accent',
    },
    notfound: {
      icon: 'search_off',
      title: 'Not Found',
      message: 'The requested resource could not be found.',
      color: 'primary',
    },
    unauthorized: {
      icon: 'lock',
      title: 'Session Expired',
      message: 'Your session has expired. Please log in again.',
      color: 'warn',
    },
    forbidden: {
      icon: 'block',
      title: 'Access Denied',
      message: 'You do not have permission to perform this action.',
      color: 'warn',
    },
    conflict: {
      icon: 'content_copy',
      title: 'Conflict',
      message: 'This action conflicts with an existing resource.',
      color: 'accent',
    },
    general: {
      icon: 'info',
      title: 'Something went wrong',
      message: 'An unexpected error occurred. Please try again.',
      color: 'primary',
    },
  };

  get errorConfig(): ErrorConfig {
    return this.configs[this.type] || this.configs.general;
  }

  get displayTitle(): string {
    return this.title || this.errorConfig.title;
  }

  get displayMessage(): string {
    return this.message || this.errorConfig.message;
  }

  get showRetryButton(): boolean {
    return (
      this.showRetry &&
      (this.type === 'network' || this.type === 'server' || this.type === 'general')
    );
  }

  onRetry(): void {
    if (!this.isRetrying) {
      this.retry.emit();
    }
  }

  onGoBack(): void {
    this.goBack.emit();
  }

  onDismiss(): void {
    this.dismiss.emit();
  }

  onCustomAction(): void {
    this.customActionClick.emit();
  }
}
