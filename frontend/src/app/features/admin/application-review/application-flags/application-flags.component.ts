import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

/**
 * Badges a company admin reads at a glance on an application: an expired document (GH-498) and a
 * returning resident (GH-499). Shows nothing when no flag is set.
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
  /** The student had been checked in at this company when they applied. */
  @Input() returning: boolean | null | undefined = false;
}
