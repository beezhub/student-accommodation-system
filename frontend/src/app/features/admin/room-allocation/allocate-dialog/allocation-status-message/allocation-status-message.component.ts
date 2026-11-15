import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * The green or red line under an allocate dialog field that says whether its check passed. The
 * caller projects the message, so each check keeps its own wording.
 */
@Component({
  selector: 'app-allocation-status-message',
  templateUrl: './allocation-status-message.component.html',
  styleUrl: './allocation-status-message.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  // The message sits in the dialog's form column as if it were the form's own element.
  host: { style: 'display: contents' },
})
export class AllocationStatusMessageComponent {
  /** True shows the green tick, false the red error. */
  readonly ok = input.required<boolean>();
}
