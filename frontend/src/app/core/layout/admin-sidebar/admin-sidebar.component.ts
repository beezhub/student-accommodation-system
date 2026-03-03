import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map, shareReplay } from 'rxjs/operators';
import { Observable } from 'rxjs';

interface NavItem {
  path: string;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './admin-sidebar.component.html',
  styleUrls: ['./admin-sidebar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSidebarComponent {
  private breakpointObserver = inject(BreakpointObserver);

  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset).pipe(
    map((result: any) => result.matches),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    shareReplay()
  );

  navItems: NavItem[] = [
    { path: '/admin', icon: 'dashboard', label: 'Dashboard' },
    { path: '/admin/applications', icon: 'assignment', label: 'Applications' },
    { path: '/admin/documents', icon: 'description', label: 'Documents' },
    { path: '/admin/users', icon: 'people', label: 'Users' },
    { path: '/admin/buildings', icon: 'apartment', label: 'Buildings' },
    { path: '/admin/rooms', icon: 'meeting_room', label: 'Rooms' },
    { path: '/admin/room-allocations', icon: 'hotel', label: 'Allocations' },
    { path: '/admin/allocations/staged', icon: 'pending_actions', label: 'Staged Alloc.' },
    { path: '/admin/waiting-list', icon: 'format_list_numbered', label: 'Waiting List' },
    { path: '/admin/accommodation-types', icon: 'category', label: 'Accomm. Types' },
    { path: '/admin/institutions', icon: 'school', label: 'Institutions' },
    { path: '/admin/year-of-study', icon: 'timeline', label: 'Years of Study' },
    { path: '/admin/academic-years', icon: 'date_range', label: 'Academic Years' },
    { path: '/admin/document-types', icon: 'folder', label: 'Doc Types' },
    { path: '/admin/allocation-rules', icon: 'rule', label: 'Alloc. Rules' },
    { path: '/admin/activity-logs', icon: 'history', label: 'Activity Logs' },
    { path: '/admin/settings', icon: 'settings', label: 'Settings' },
  ];
}
