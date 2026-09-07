import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';

export interface ReasonDialogData {
  title: string;
  /** Shown above the field: what the reader will do with this text. */
  message?: string;
  label: string;
  placeholder?: string;
  confirmLabel?: string;
  /** Shortest reason worth sending. Defaults to 10 characters. */
  minLength?: number;
}

/**
 * Collects a free-text reason, replacing native `prompt` (GH-157).
 *
 * <p>`prompt` returned whatever was typed, including a single space, and the caller only checked
 * for a falsy value — so "requesting changes" could reach a student as a blank explanation of what
 * to change. The length rule here is the validation the issue asks for; it is deliberately a
 * minimum rather than a pattern, because there is no shape a reason has to take.
 *
 * <p>Returns `null` when dismissed, so a caller can tell "cancelled" from "submitted".
 */
@Component({
  selector: 'app-reason-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>

    <mat-dialog-content>
      @if (data.message) {
      <p class="message">{{ data.message }}</p>
      }
      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ data.label }}</mat-label>
          <textarea
            matInput
            rows="4"
            cdkFocusInitial
            formControlName="reason"
            [placeholder]="data.placeholder || ''"
          ></textarea>
          @if (form.controls['reason'].hasError('required')) {
          <mat-error>A reason is required</mat-error>
          } @if (form.controls['reason'].hasError('minlength')) {
          <mat-error>Give at least {{ minLength }} characters, so it is actionable</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(null)">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="form.invalid"
        (click)="dialogRef.close(form.value.reason.trim())"
      >
        {{ data.confirmLabel || 'Submit' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .full-width {
        width: 100%;
      }
      .message {
        margin: 0 0 12px;
      }
    `,
  ],
})
export class ReasonDialogComponent {
  readonly dialogRef = inject<MatDialogRef<ReasonDialogComponent, string | null>>(MatDialogRef);
  readonly data = inject<ReasonDialogData>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  readonly minLength = this.data.minLength ?? 10;

  form: FormGroup = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(this.minLength)]],
  });
}

/** Opens the dialog; emits the trimmed reason, or `null` if the user cancelled. */
export function askForReason(dialog: MatDialog, data: ReasonDialogData): Observable<string | null> {
  return new Observable<string | null>((subscriber) => {
    const ref = dialog.open(ReasonDialogComponent, { data, width: '480px' });
    const sub = ref.afterClosed().subscribe((result) => {
      subscriber.next(result ?? null);
      subscriber.complete();
    });
    return () => sub.unsubscribe();
  });
}
