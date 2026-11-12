import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ContactInfoService } from '../../../core/services/contact-info.service';
import { telHref } from '../../../core/models/contact-info.model';

/**
 * Shows how to reach support. It used to be a form whose Send button only showed a "sent"
 * message: nothing was delivered, so a student who used it was never answered.
 */
@Component({
  selector: 'app-contact-support',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './contact-support.component.html',
  styleUrls: ['./contact-support.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactSupportComponent {
  /** undefined while loading, null when the details could not be loaded. */
  readonly contact = toSignal(inject(ContactInfoService).getContactInfo(), {
    initialValue: undefined,
  });
  readonly telHref = telHref;
}
