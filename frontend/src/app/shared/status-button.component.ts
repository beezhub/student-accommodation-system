import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-status-button',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  template: `
    <button mat-raised-button [ngClass]="statusClass" class="status-btn">
      <mat-icon *ngIf="icon">{{ icon }}</mat-icon>
      {{ status }}
    </button>
  `,
  styleUrls: ['./status-button.component.css'],
})
export class StatusButtonComponent {
  @Input() status: string = 'Pending';

  get statusClass(): string {
    switch (this.status.toLowerCase()) {
      case 'verified':
        return 'status-verified';
      case 'pending':
        return 'status-pending';
      case 'rejected':
        return 'status-rejected';
      default:
        return '';
    }
  }

  get icon(): string {
    switch (this.status.toLowerCase()) {
      case 'verified':
        return 'check_circle';
      case 'pending':
        return 'hourglass_empty';
      case 'rejected':
        return 'cancel';
      default:
        return '';
    }
  }
}
