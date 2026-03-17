import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatNativeDateModule } from '@angular/material/core';
import { RoomAllocationResponse } from '../../../core/services/room-allocation.service';

export interface AcceptOfferDialogData {
  allocation: RoomAllocationResponse;
}

@Component({
  selector: 'app-accept-offer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatNativeDateModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title>Accept Room Offer</h2>
        <p class="text-muted">
          Confirm your expected check-in date to secure your room. Check-in must occur within 7 days
          of the offer date.
        </p>
      </div>
      <mat-dialog-content>
        <form [formGroup]="form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Expected Check-in Date</mat-label>
            <input
              matInput
              [matDatepicker]="picker"
              formControlName="expectedCheckInDate"
              [min]="minDate"
              [max]="maxDate"
            />
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
            @if (form.get('expectedCheckInDate')?.hasError('required')) {
            <mat-error>Date is required</mat-error>
            }
          </mat-form-field>
        </form>
      </mat-dialog-content>
      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="submit()">
          Accept Offer
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
      .text-muted {
        margin: 0;
        color: #64748b;
        font-size: 15px;
        line-height: 1.5;
      }
      .full-width {
        width: 100%;
      }
      mat-dialog-content {
        min-width: 380px;
        padding-top: 8px !important;
        overflow: visible;
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
export class AcceptOfferDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<AcceptOfferDialogComponent>>(MatDialogRef);
  data = inject<AcceptOfferDialogData>(MAT_DIALOG_DATA);

  form: FormGroup;
  minDate: Date;
  maxDate: Date;

  constructor() {
    const data = this.data;

    // Calculate dates based on when the room was offered (createdAt)
    const offerDate = new Date(data.allocation.createdAt);
    const today = new Date();

    // Reset time to midnight for accurate date comparisons
    today.setHours(0, 0, 0, 0);
    offerDate.setHours(0, 0, 0, 0);

    // Min date is the later of today or offer date (can't check in to the past)
    this.minDate = today > offerDate ? today : offerDate;

    // Max date is 7 days from the offer date
    this.maxDate = new Date(offerDate);
    this.maxDate.setDate(this.maxDate.getDate() + 7);

    this.form = this.fb.group({
      expectedCheckInDate: [null, Validators.required],
    });
  }

  submit() {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value.expectedCheckInDate);
    }
  }
}
