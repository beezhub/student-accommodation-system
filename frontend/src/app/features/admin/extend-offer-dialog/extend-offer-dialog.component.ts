import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RoomAllocationService } from '../../../core/services/room-allocation.service';

export interface ExtendOfferDialogData {
  allocationId: number;
  studentName: string;
  roomNumber: string;
  buildingName: string;
  currentDeadline?: string;
}

/**
 * Moves an outstanding offer's response deadline (GH-198).
 *
 * <p>Enforcing the deadline on the accept path was right, but left a lapsed offer with no recovery
 * route: nothing wrote `offerExpiryDate` after the offer was made, and the guard blocked an admin
 * accepting on the student's behalf too. Extending hands the decision back to the student rather
 * than taking it from them.
 *
 * <p>A dialog rather than a native prompt: the reason is mandatory and goes into the audit trail,
 * and GH-157 is already about removing the `confirm`/`prompt` calls elsewhere on this screen.
 */
@Component({
  selector: 'app-extend-offer-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Extend offer deadline</h2>

    <mat-dialog-content>
      <p class="context">
        {{ data.studentName }} — Room {{ data.roomNumber }}, {{ data.buildingName }}
      </p>
      @if (data.currentDeadline) {
      <p class="current-deadline">
        <mat-icon inline>schedule</mat-icon>
        Current deadline: {{ data.currentDeadline | date : 'd MMM y, HH:mm' }}
      </p>
      }

      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>New deadline</mat-label>
          <input
            matInput
            [matDatepicker]="picker"
            [min]="minDate"
            [max]="maxDate"
            formControlName="newExpiryDate"
          />
          <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
          <mat-hint>Must be in the future, and no later than a fresh offer would give</mat-hint>
          @if (form.get('newExpiryDate')?.hasError('required')) {
          <mat-error>Choose the new deadline</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Reason</mat-label>
          <textarea
            matInput
            rows="3"
            formControlName="reason"
            placeholder="Why is the deadline moving?"
          ></textarea>
          <mat-hint>Recorded against the allocation</mat-hint>
          @if (form.get('reason')?.hasError('required')) {
          <mat-error>A reason is required</mat-error>
          } @if (form.get('reason')?.hasError('minlength')) {
          <mat-error>Give at least a few words</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" [disabled]="isSubmitting">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        (click)="onSubmit()"
        [disabled]="form.invalid || isSubmitting"
      >
        @if (isSubmitting) {
        <mat-spinner diameter="18"></mat-spinner>
        } @else { Extend deadline }
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .full-width {
        width: 100%;
      }
      .context {
        margin: 0 0 4px;
        font-weight: 500;
      }
      .current-deadline {
        margin: 0 0 16px;
        opacity: 0.75;
        font-size: 0.9rem;
      }
    `,
  ],
})
export class ExtendOfferDialogComponent {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<ExtendOfferDialogComponent>>(MatDialogRef);
  private allocationService = inject(RoomAllocationService);
  private snackBar = inject(MatSnackBar);
  data = inject<ExtendOfferDialogData>(MAT_DIALOG_DATA);

  form: FormGroup;
  isSubmitting = false;

  /** Tomorrow at the earliest; the backend refuses anything not in the future. */
  readonly minDate = new Date(Date.now() + 24 * 60 * 60 * 1000);

  /**
   * The backend caps an extension at what a fresh offer would give, so a mistyped year cannot hold
   * a room out of the pool for decades. That ceiling is `app.allocation.offer-expiry-days`, which
   * the frontend cannot read; 30 days is a deliberately generous stand-in, and the backend remains
   * the authority — it will refuse anything past its own limit with a message naming it.
   */
  readonly maxDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);

  constructor() {
    this.form = this.fb.group({
      newExpiryDate: [null, Validators.required],
      reason: ['', [Validators.required, Validators.minLength(3)]],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.isSubmitting = true;

    const chosen: Date = this.form.value.newExpiryDate;
    // The picker yields midnight. End of day is what an admin means by "they have until the 5th",
    // and it is also the safer reading: it never shortens the window they picked.
    const deadline = new Date(chosen);
    deadline.setHours(23, 59, 0, 0);

    this.allocationService
      .extendOffer(this.data.allocationId, toLocalDateTime(deadline), this.form.value.reason)
      .subscribe({
        next: () => {
          this.snackBar.open('Offer deadline extended', 'Close', { duration: 3000 });
          this.dialogRef.close(true);
        },
        error: (error) => {
          this.isSubmitting = false;
          const message: string | undefined = error?.error?.errors?.[0];
          this.snackBar.open(message || 'Failed to extend the offer deadline', 'Close', {
            duration: 6000,
          });
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}

/**
 * Serialises for the backend's `LocalDateTime`, which carries no zone.
 *
 * <p>`toISOString` would convert to UTC first, so an admin on a positive offset would send the
 * previous day and silently shorten the window they chose.
 */
function toLocalDateTime(date: Date): string {
  const pad = (value: number): string => String(value).padStart(2, '0');
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
  );
}
