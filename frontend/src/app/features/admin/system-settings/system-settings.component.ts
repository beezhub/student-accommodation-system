import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RoleService } from '../../../core/services/role.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-system-settings',
  templateUrl: './system-settings.component.html',
  styleUrls: ['./system-settings.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
})
export class SystemSettingsComponent {
  protected readonly roleService = inject(RoleService);

  /**
   * The auto-allocation switch is an FF4j feature flag, and that screen is platform-admin only.
   * A tenant admin is told who owns it rather than shown a link they cannot follow.
   *
   * <p>This card used to be a toggle writing the `auto_allocation_enabled` setting, which gated
   * only the per-application path while the feature flag gated the batch path (GH-207).
   */
  get canManageFeatureFlags(): boolean {
    return this.roleService.isPlatformAdmin();
  }

  settingsCategories: {
    title: string;
    description: string;
    icon: string;
    route: string;
    class: string;
    platformOnly?: boolean;
  }[] = [
    {
      title: 'Accommodation Types',
      description: 'Manage accommodation type options',
      icon: 'hotel',
      route: '/admin/accommodation-types',
      class: 'accommodation-types',
    },
    {
      title: 'Document Types',
      description: 'Configure document types required for student applications',
      icon: 'description',
      route: '/admin/document-types',
      class: 'documents',
    },
    {
      title: 'Institutions',
      description: 'Manage partner educational institutions',
      icon: 'school',
      route: '/admin/institutions',
      class: 'institutions',
    },
    {
      title: 'Year of Study',
      description: 'Configure available years of study',
      icon: 'calendar_today',
      route: '/admin/year-of-study',
      class: 'years',
    },
    {
      title: 'Feature Flags',
      description: 'Toggle FF4J feature flags for the system',
      icon: 'toggle_on',
      route: '/admin/feature-flags',
      class: 'feature-flags',
      // Its route is PlatformAdminGuard-protected, so a tenant admin who clicked this would be
      // bounced straight back here. Now that they can reach this page, do not offer it (GH-158).
      platformOnly: true,
    },
  ];

  /** The cards this admin can actually open. */
  get visibleSettingsCategories() {
    return this.settingsCategories.filter(
      (category) => !category.platformOnly || this.roleService.isPlatformAdmin()
    );
  }
}
