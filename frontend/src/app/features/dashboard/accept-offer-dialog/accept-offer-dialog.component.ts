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

  /**
   * Matches app.allocation.check-in-expiry-days on the backend, which is not exposed to the
   * client. If that setting is ever changed, this silently goes out of step.
   */
  private static readonly CHECK_IN_WINDOW_DAYS = 7;

  constructor() {
    // The window runs from acceptance, not from when the offer was made. Deriving it from
    // createdAt gave a student who accepted late a needlessly narrow picker, and if the offer
    // window were ever configured longer than the check-in window, maxDate fell before minDate
    // and no date was selectable at all, locking the student out of accepting (GH-151).
    //
    // Binding maxDate to offerExpiryDate instead, as the issue suggests, has the same failure:
    // that is the deadline to respond, not the check-in window, so on the last day of an offer
    // it collapses to nothing. It is used for the informational line only.
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    this.minDate = today;

    // One day short of the full window. The emitted time is 23:59, and the backend compares
    // against now + 7 days at the current time of day, so offering today+7 would be rejected for
    // any acceptance before 23:59.
    this.maxDate = new Date(today);
    this.maxDate.setDate(
      this.maxDate.getDate() + AcceptOfferDialogComponent.CHECK_IN_WINDOW_DAYS - 1
    );

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
