import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Observable } from 'rxjs';

export interface ConfirmDialogData {
  title: string;
  message: string;
  /** Label on the button that goes ahead. Say what happens, not "OK" (GH-157). */
  confirmLabel?: string;
  cancelLabel?: string;
  /** Colours the confirm button as a warning and shows the alert icon. */
  destructive?: boolean;
}

/**
 * The confirmation prompt for destructive admin actions, replacing native `confirm` (GH-157).
 *
 * <p>`confirm` blocks the browser's event loop, cannot be styled, reads to a screen reader as a
 * bare string with no title or role, and is suppressed outright by some browsers when a page opens
 * several — so an admin could click delete and simply have nothing happen. It is also untestable
 * without stubbing a global.
 *
 * <p>Cancel is focused rather than confirm, so return on a dialog that appeared unexpectedly is
 * the safe answer.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>
      @if (data.destructive) {
      <mat-icon class="warn-icon" aria-hidden="true">warning</mat-icon>
      }
      {{ data.title }}
    </h2>

    <mat-dialog-content>
      <p class="message">{{ data.message }}</p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button cdkFocusInitial (click)="dialogRef.close(false)">
        {{ data.cancelLabel || 'Cancel' }}
      </button>
      <button
        mat-flat-button
        [color]="data.destructive ? 'warn' : 'primary'"
        (click)="dialogRef.close(true)"
      >
        {{ data.confirmLabel || 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .message {
        margin: 0;
        white-space: pre-line;
      }
      .warn-icon {
        vertical-align: middle;
        margin-right: 4px;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly dialogRef = inject<MatDialogRef<ConfirmDialogComponent, boolean>>(MatDialogRef);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}

/**
 * Opens the dialog and emits whether the user went ahead.
 *
 * <p>A helper because the call sites replacing `confirm` are one-liners inside an `if`, and
 * repeating the `MatDialog.open(...).afterClosed()` shape at each of them is what would make
 * people reach for `confirm` again. Emits `false`, never `undefined`, so callers can treat a
 * dismissal by backdrop or Escape as a refusal without a truthiness check.
 */
export function confirmAction(dialog: MatDialog, data: ConfirmDialogData): Observable<boolean> {
  return new Observable<boolean>((subscriber) => {
    const ref = dialog.open(ConfirmDialogComponent, {
      data,
      width: '420px',
      autoFocus: false,
      restoreFocus: true,
    });
    const sub = ref.afterClosed().subscribe((result) => {
      subscriber.next(result === true);
      subscriber.complete();
    });
    return () => sub.unsubscribe();
  });
}
