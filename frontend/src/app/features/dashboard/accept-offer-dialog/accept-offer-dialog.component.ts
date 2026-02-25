import { Component, Inject } from '@angular/core';
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
    <h2 mat-dialog-title>Accept Room Offer</h2>
    <mat-dialog-content>
      <p>
        Please select your expected check-in date. This must be within 7 days from when the room was
        offered.
      </p>
      <form [formGroup]="form">
        <mat-form-field appearance="fill" class="full-width">
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
          <mat-error *ngIf="form.get('expectedCheckInDate')?.hasError('required')"
            >Date is required</mat-error
          >
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="submit()">
        Accept Offer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .full-width {
        width: 100%;
      }
      mat-dialog-content {
        padding-top: 10px;
        min-width: 300px;
      }
    `,
  ],
})
export class AcceptOfferDialogComponent {
  form: FormGroup;
  minDate: Date;
  maxDate: Date;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AcceptOfferDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AcceptOfferDialogData
  ) {
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
