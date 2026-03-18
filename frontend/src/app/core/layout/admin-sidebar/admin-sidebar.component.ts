import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map, shareReplay } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { BrandLogoComponent } from '../../../shared/components/brand-logo/brand-logo.component';

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [
    AsyncPipe,
    RouterModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    BrandLogoComponent,
  ],
  templateUrl: './admin-sidebar.component.html',
  styleUrls: ['./admin-sidebar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSidebarComponent {
  private breakpointObserver = inject(BreakpointObserver);
  private authService = inject(AuthService);

  currentUser$ = this.authService.currentUser;

  logout(): void {
    this.authService.logout();
  }

  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset).pipe(
    map((result: any) => result.matches),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shareReplay()
  );

  navGroups: NavGroup[] = [
    {
      label: 'Overview',
      items: [{ path: '/admin', icon: 'dashboard', label: 'Dashboard' }],
    },
    {
      label: 'Applications',
      items: [
        { path: '/admin/applications', icon: 'assignment', label: 'Applications' },
        { path: '/admin/room-allocations', icon: 'hotel', label: 'Allocations' },
        { path: '/admin/allocations/staged', icon: 'pending_actions', label: 'Staged Allocations' },
        { path: '/admin/waiting-list', icon: 'format_list_numbered', label: 'Waiting List' },
      ],
    },
    {
      label: 'Buildings & Rooms',
      items: [
        { path: '/admin/buildings', icon: 'apartment', label: 'Buildings' },
        { path: '/admin/rooms', icon: 'meeting_room', label: 'Rooms' },
      ],
    },
    {
      label: 'Configuration',
      items: [
{ path: '/admin/accommodation-types', icon: 'category', label: 'Accomm. Types' },
        { path: '/admin/institutions', icon: 'school', label: 'Institutions' },
        { path: '/admin/year-of-study', icon: 'timeline', label: 'Years of Study' },
        { path: '/admin/academic-years', icon: 'date_range', label: 'Academic Years' },
        { path: '/admin/document-types', icon: 'folder', label: 'Doc Types' },
        { path: '/admin/settings', icon: 'settings', label: 'System Settings' },
        { path: '/admin/feature-flags', icon: 'toggle_on', label: 'Feature Flags' },
      ],
    },
    {
      label: 'Administration',
      items: [
        { path: '/admin/users', icon: 'people', label: 'Users' },
        { path: '/admin/owner-registrations', icon: 'business', label: 'Owner Registrations' },
        { path: '/admin/documents', icon: 'description', label: 'Documents' },
        { path: '/admin/activity-logs', icon: 'history', label: 'Activity Logs' },
      ],
    },
  ];
}
