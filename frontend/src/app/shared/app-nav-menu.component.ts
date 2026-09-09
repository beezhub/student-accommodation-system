import { Component, Output, EventEmitter, inject } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { RouterModule } from '@angular/router';

import { AuthService } from '../core/services/auth.service';
import { RoleService } from '../core/services/role.service';
import { NotificationsComponent } from './notifications/notifications.component';

@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [
    MatMenuModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    RouterModule,
    NotificationsComponent,
  ],
  template: `
    <nav class="student-nav">
      <!-- Desktop nav links -->
      @if (isStudent()) {
      <div class="nav-links">
        <a class="nav-link" routerLink="/dashboard" routerLinkActive="nav-link--active">
          <mat-icon>grid_view</mat-icon>
          <span>Dashboard</span>
        </a>
        <a class="nav-link" routerLink="/applications" routerLinkActive="nav-link--active">
          <mat-icon>description</mat-icon>
          <span>Applications</span>
        </a>
        <a class="nav-link" routerLink="/documents" routerLinkActive="nav-link--active">
          <mat-icon>folder_open</mat-icon>
          <span>Documents</span>
        </a>
      </div>
      }

      <!-- Right actions -->
      <div class="nav-right">
        <app-notifications></app-notifications>

        <!-- User avatar — desktop -->
        <button
          class="nav-avatar desktop-only"
          [matMenuTriggerFor]="profileMenu"
          aria-label="Open profile menu"
        >
          {{ initials }}
        </button>

        <!-- Hamburger — mobile -->
        <button
          class="nav-hamburger mobile-only"
          [matMenuTriggerFor]="mobileMenu"
          aria-label="Open navigation menu"
        >
          <mat-icon>menu</mat-icon>
        </button>
      </div>
    </nav>

    <!-- ── Profile dropdown ─────────────────────────────────────────────── -->
    <mat-menu #profileMenu="matMenu" xPosition="before">
      <div class="nav-menu-user" (click)="$event.stopPropagation()">
        <span class="nav-menu-name">{{ currentUserName }}</span>
        <span class="nav-menu-email">{{ currentUserEmail }}</span>
      </div>
      @if (isStudent()) {
      <button mat-menu-item routerLink="/profile">
        <mat-icon>person_outline</mat-icon>
        My Profile
      </button>
      }
      <button mat-menu-item routerLink="/change-password">
        <mat-icon>lock_outline</mat-icon>
        Change Password
      </button>
      <mat-divider></mat-divider>
      <button mat-menu-item class="nav-menu-logout" (click)="logout.emit()">
        <mat-icon>logout</mat-icon>
        Sign Out
      </button>
    </mat-menu>

    <!-- ── Mobile menu ──────────────────────────────────────────────────── -->
    <mat-menu #mobileMenu="matMenu" xPosition="before">
      @if (isStudent()) {
      <button mat-menu-item routerLink="/dashboard">
        <mat-icon>grid_view</mat-icon>
        Dashboard
      </button>
      <button mat-menu-item routerLink="/applications">
        <mat-icon>description</mat-icon>
        Applications
      </button>
      <button mat-menu-item routerLink="/documents">
        <mat-icon>folder_open</mat-icon>
        Documents
      </button>
      <mat-divider></mat-divider>
      <button mat-menu-item routerLink="/profile">
        <mat-icon>person_outline</mat-icon>
        My Profile
      </button>
      }
      <button mat-menu-item routerLink="/change-password">
        <mat-icon>lock_outline</mat-icon>
        Change Password
      </button>
      <mat-divider></mat-divider>
      <button mat-menu-item (click)="logout.emit()">
        <mat-icon>logout</mat-icon>
        Sign Out
      </button>
    </mat-menu>
  `,
  styleUrls: ['./app-nav-menu.component.scss'],
})
export class AppNavMenuComponent {
  private authService = inject(AuthService);
  private roleService = inject(RoleService);

  @Output() logout = new EventEmitter<void>();

  get initials(): string {
    const user = this.authService.currentUserValue;
    if (!user) return '?';
    return ((user.firstName?.[0] ?? '') + (user.lastName?.[0] ?? '')).toUpperCase();
  }

  get currentUserName(): string {
    const user = this.authService.currentUserValue;
    if (!user) return '';
    return `${user.firstName} ${user.lastName}`;
  }

  get currentUserEmail(): string {
    return this.authService.currentUserValue?.email ?? '';
  }

  isStudent(): boolean {
    return this.roleService.isStudent();
  }
}
