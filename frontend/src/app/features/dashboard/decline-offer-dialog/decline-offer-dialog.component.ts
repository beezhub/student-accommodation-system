import { Component, OnInit, inject } from '@angular/core';
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
    MatSelectModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title>Decline Offer</h2>
        <p class="subtitle">
          <span class="room-info">{{ data.roomNumber }} • {{ data.buildingName }}</span>
        </p>
      </div>
      <mat-dialog-content>
        <div class="status-card" [class.is-final]="declinesRemaining === 0">
          <div class="status-content">
            <div class="status-title">
              {{ declinesRemaining === 0 ? 'Final Decline Warning' : 'Decline Status' }}
            </div>
            <p class="status-desc">
              You have declined {{ data.previousDeclineCount }} of 2 offers.
              <strong
                >{{ declinesRemaining }} decline{{
                  declinesRemaining === 1 ? '' : 's'
                }}
                remaining.</strong
              >
            </p>
            @if (declinesRemaining > 0) {
            <p class="status-note">
              You will be re-queued and notified when another room becomes available.
            </p>
            } @if (declinesRemaining === 0) {
            <p class="status-alert">
              This is your final decline. Your application will be closed and removed from the
              waiting list.
            </p>
            }
          </div>
        </div>

        <form [formGroup]="form" class="decline-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reason for Declining *</mat-label>
            <mat-select formControlName="declineReason" required>
              @for (reason of declineReasons; track reason.value) {
              <mat-option [value]="reason.value">
                {{ reason.label }}
              </mat-option>
              }
            </mat-select>
            @if (form.get('declineReason')?.hasError('required')) {
            <mat-error>Please select a reason</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Additional Details (Optional)</mat-label>
            <textarea
              matInput
              formControlName="declineReasonText"
              rows="4"
              maxlength="500"
              placeholder="Provide any additional context..."
            ></textarea>
            <mat-hint align="end"
              >{{ form.get('declineReasonText')?.value?.length || 0 }}/500</mat-hint
            >
          </mat-form-field>
        </form>
      </mat-dialog-content>
      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button mat-dialog-close>Cancel</button>
        <button mat-flat-button color="warn" [disabled]="form.invalid" (click)="submit()">
          {{ declinesRemaining === 0 ? 'Decline & Close Application' : 'Decline Offer' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        padding: 16px 8px;
      }
      .dialog-header {
        padding: 0 24px;
        margin-bottom: 24px;
      }
      .dialog-header h2 {
        margin: 0 0 8px 0;
        font-size: 24px;
        font-weight: 600;
        color: #0f172a;
      }
      .subtitle {
        margin: 0;
        color: #64748b;
        font-size: 15px;
      }
      .room-info {
        display: inline-block;
        background: #f1f5f9;
        color: #475569;
        padding: 4px 12px;
        border-radius: 999px;
        font-weight: 500;
        font-size: 14px;
        margin-top: 4px;
      }
      mat-dialog-content {
        min-width: 420px;
        max-width: 500px;
        padding-top: 8px !important;
      }

      /* Restrained, premium status card instead of generic yellow/red banner */
      .status-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 16px 20px;
        margin-bottom: 24px;
      }
      .status-card.is-final {
        background: #fef2f2;
        border-color: #fecaca;
      }
      .status-title {
        font-weight: 600;
        color: #0f172a;
        margin-bottom: 8px;
        font-size: 15px;
      }
      .status-card.is-final .status-title {
        color: #991b1b;
      }
      .status-desc {
        margin: 0 0 4px 0;
        color: #475569;
        font-size: 14px;
      }
      .status-note {
        margin: 8px 0 0 0;
        color: #64748b;
        font-size: 14px;
      }
      .status-alert {
        margin: 8px 0 0 0;
        color: #991b1b;
        font-size: 14px;
        font-weight: 500;
      }

      .full-width {
        width: 100%;
        margin-bottom: 8px;
      }
      textarea {
        resize: none;
      }
      .dialog-actions {
        padding: 16px 24px 0 24px;
        margin-bottom: -8px;
      }
      button {
        padding: 8px 24px;
        letter-spacing: 0.02em;
      }
    `,
  ],
})
export class DeclineOfferDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<DeclineOfferDialogComponent>>(MatDialogRef);
  data = inject<DeclineOfferDialogData>(MAT_DIALOG_DATA);

  form: FormGroup;
  declinesRemaining: number;

  declineReasons = [
    { value: DeclineReason.WRONG_LOCATION, label: 'Wrong Location' },
    { value: DeclineReason.TOO_EXPENSIVE, label: 'Too Expensive' },
    { value: DeclineReason.FOUND_ALTERNATIVE, label: 'Found Alternative Accommodation' },
    { value: DeclineReason.ACCESSIBILITY_ISSUES, label: 'Accessibility Issues' },
    { value: DeclineReason.PERSONAL_REASONS, label: 'Personal Reasons' },
    { value: DeclineReason.OTHER, label: 'Other' },
  ];

  constructor() {
    this.declinesRemaining = 2 - this.data.previousDeclineCount;
    this.form = this.fb.group({
      declineReason: [null, Validators.required],
      declineReasonText: ['', Validators.maxLength(500)],
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
        declineReasonText: this.form.value.declineReasonText || undefined,
      };
      this.dialogRef.close(request);
    }
  }
}
