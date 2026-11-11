import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

/**
 * Badges a company admin reads at a glance on an application: an expired document (GH-498).
 * Shows nothing when no flag is set.
 */
@Component({
  selector: 'app-application-flags',
  templateUrl: './application-flags.component.html',
  styleUrls: ['./application-flags.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule, MatTooltipModule],
})
export class ApplicationFlagsComponent {
  /** An expiring document of the student has expired at this company. */
  @Input() expired: boolean | null | undefined = false;
}
