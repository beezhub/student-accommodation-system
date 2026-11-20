import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ContactInfoService } from '../../../core/services/contact-info.service';
import { RoleService } from '../../../core/services/role.service';
import { telHref } from '../../../core/models/contact-info.model';

/**
 * The privacy policy students accept at signup (#559). Public, like /buildings, so it can be read
 * before an account exists; the signup form opens it in a new tab so a half-filled form survives.
 *
 * The text is in the template. The contact details come from GET /api/contact, the same
 * APP_SUPPORT_EMAIL and APP_CONTACT_PHONE every other page and every email footer shows, so one
 * setting per deployment controls them.
 */
@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [RouterModule, MatIconModule],
  templateUrl: './privacy-policy.component.html',
  styleUrls: ['./privacy-policy.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPolicyComponent {
  /** undefined while loading, null when the details could not be loaded. */
  readonly contact = toSignal(inject(ContactInfoService).getContactInfo(), {
    initialValue: undefined,
  });
  readonly telHref = telHref;

  /**
   * The app navbar is hidden here, as on every public page, so the page offers its own way back:
   * to the dashboard for a signed-in student, to the home page for everyone else.
   */
  readonly back = inject(RoleService).isStudent()
    ? { link: '/dashboard', label: 'Back to dashboard' }
    : { link: '/home', label: 'Back to home' };
}
