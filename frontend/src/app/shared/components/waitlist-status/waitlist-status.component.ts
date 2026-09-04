import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { WaitlistPosition } from '../../../core/services/waitlist.service';

/**
 * Presentational waitlist transparency card. Renders position, queue depth,
 * an optional estimated wait time, and a notification reassurance message.
 * Purely data-driven — visibility is controlled by the parent (hidden when
 * `position` is null, e.g. after a 404 from the waitlist API).
 */
@Component({
  selector: 'app-waitlist-status',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './waitlist-status.component.html',
  styleUrls: ['./waitlist-status.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WaitlistStatusComponent {
  @Input({ required: true }) position!: WaitlistPosition;
}
