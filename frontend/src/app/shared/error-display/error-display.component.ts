import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

export type ErrorType = 'network' | 'server' | 'validation' | 'notfound' | 'unauthorized' | 'general';

@Component({
  selector: 'app-error-display',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatCardModule],
  templateUrl: './error-display.component.html',
  styleUrls: ['./error-display.component.css']
})
export class ErrorDisplayComponent {
  @Input() type: ErrorType = 'general';
  @Input() title?: string;
  @Input() message?: string;
  @Input() showRetry = true;
  @Input() showGoBack = false;
  @Input() customAction?: string;
  @Output() retry = new EventEmitter<void>();
  @Output() goBack = new EventEmitter<void>();
  @Output() customActionClick = new EventEmitter<void>();

  get errorConfig() {
    const configs = {
      network: {
        icon: 'cloud_off',
        title: 'Connection Error',
        message: 'Unable to connect to the server. Please check your internet connection and try again.',
        color: 'warn'
      },
      server: {
        icon: 'error_outline',
        title: 'Server Error',
        message: 'A server error occurred. Our team has been notified. Please try again later.',
        color: 'warn'
      },
      validation: {
        icon: 'warning',
        title: 'Invalid Input',
        message: 'Please check your input and try again.',
        color: 'accent'
      },
      notfound: {
        icon: 'search_off',
        title: 'Not Found',
        message: 'The page or resource you are looking for could not be found.',
        color: 'primary'
      },
      unauthorized: {
        icon: 'lock',
        title: 'Access Denied',
        message: 'You do not have permission to access this resource.',
        color: 'warn'
      },
      general: {
        icon: 'info',
        title: 'Something went wrong',
        message: 'An unexpected error occurred. Please try again.',
        color: 'primary'
      }
    };

    return configs[this.type] || configs.general;
  }

  get displayTitle(): string {
    return this.title || this.errorConfig.title;
  }

  get displayMessage(): string {
    return this.message || this.errorConfig.message;
  }

  onRetry(): void {
    this.retry.emit();
  }

  onGoBack(): void {
    this.goBack.emit();
  }

  onCustomAction(): void {
    this.customActionClick.emit();
  }
}
