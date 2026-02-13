import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { DeclineReason, DeclineOfferRequest } from '../../../core/services/room-allocation.service';

export interface DeclineOfferDialogData {
  previousDeclineCount: number;
  roomNumber: string;
  buildingName: string;
}

@Component({
  selector: 'app-decline-offer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule
  ],
  template: `
    <h2 mat-dialog-title>Decline Room Offer</h2>
    <mat-dialog-content>
      <p><strong>Room:</strong> {{ data.roomNumber }} - {{ data.buildingName }}</p>

      <div class="warning-banner" [class.final-warning]="declinesRemaining === 0">
        <p><strong>⚠️ Decline Status:</strong></p>
        <p>You have declined <strong>{{ data.previousDeclineCount }} of 2</strong> offers.</p>
        <p><strong>{{ declinesRemaining }} decline(s) remaining.</strong></p>
        <p *ngIf="declinesRemaining > 0" class="info-text">
          You will be re-queued and notified when another room becomes available.
        </p>
        <p *ngIf="declinesRemaining === 0" class="final-warning-text">
          ⛔ <strong>This is your last decline. Your application will be closed and you will be removed from the waiting list.</strong>
        </p>
      </div>

      <form [formGroup]="form">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Reason for Declining *</mat-label>
          <mat-select formControlName="declineReason" required>
            <mat-option *ngFor="let reason of declineReasons" [value]="reason.value">
              {{ reason.label }}
            </mat-option>
          </mat-select>
          <mat-error *ngIf="form.get('declineReason')?.hasError('required')">
            Please select a reason
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Additional Details (Optional)</mat-label>
          <textarea
            matInput
            formControlName="declineReasonText"
            rows="3"
            maxlength="500"
            placeholder="Please provide any additional information...">
          </textarea>
          <mat-hint align="end">{{ form.get('declineReasonText')?.value?.length || 0 }}/500</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button
        mat-raised-button
        color="warn"
        [disabled]="form.invalid"
        (click)="submit()">
        {{ declinesRemaining === 0 ? 'Decline (Final)' : 'Decline Offer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }
    mat-dialog-content {
      padding-top: 10px;
      min-width: 400px;
      max-width: 500px;
    }
    .warning-banner {
      background: #fff3cd;
      border-left: 4px solid #ffc107;
      padding: 12px;
      margin: 16px 0;
      border-radius: 4px;
    }
    .warning-banner.final-warning {
      background: #f8d7da;
      border-left: 4px solid #dc3545;
    }
    .warning-banner p {
      margin: 4px 0;
    }
    .info-text {
      color: #856404;
      font-size: 0.9em;
      margin-top: 8px;
    }
    .final-warning-text {
      color: #721c24;
      font-weight: 500;
      margin-top: 8px;
    }
    textarea {
      resize: vertical;
    }
  `]
})
export class DeclineOfferDialogComponent implements OnInit {
  form: FormGroup;
  declinesRemaining: number;

  declineReasons = [
    { value: DeclineReason.WRONG_LOCATION, label: 'Wrong Location' },
    { value: DeclineReason.TOO_EXPENSIVE, label: 'Too Expensive' },
    { value: DeclineReason.FOUND_ALTERNATIVE, label: 'Found Alternative Accommodation' },
    { value: DeclineReason.ACCESSIBILITY_ISSUES, label: 'Accessibility Issues' },
    { value: DeclineReason.PERSONAL_REASONS, label: 'Personal Reasons' },
    { value: DeclineReason.OTHER, label: 'Other' }
  ];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<DeclineOfferDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeclineOfferDialogData
  ) {
    this.declinesRemaining = 2 - this.data.previousDeclineCount;
    this.form = this.fb.group({
      declineReason: [null, Validators.required],
      declineReasonText: ['', Validators.maxLength(500)]
    });
  }

  ngOnInit(): void {
    // Focus on the select field after a short delay
    setTimeout(() => {
      const selectElement = document.querySelector('mat-select');
      if (selectElement) {
        (selectElement as HTMLElement).focus();
      }
    }, 100);
  }

  submit(): void {
    if (this.form.valid) {
      const request: DeclineOfferRequest = {
        declineReason: this.form.value.declineReason,
        declineReasonText: this.form.value.declineReasonText || undefined
      };
      this.dialogRef.close(request);
    }
  }
}
