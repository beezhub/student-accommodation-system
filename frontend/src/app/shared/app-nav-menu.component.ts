import { Component, Output, EventEmitter, inject } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

import { AuthService } from '../core/services/auth.service';
import { NotificationsComponent } from './notifications/notifications.component';

@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [MatMenuModule, MatIconModule, MatButtonModule, RouterModule, NotificationsComponent],
  template: `
    <nav class="nav-bar">
      <app-notifications></app-notifications>
      <button
        mat-icon-button
        [matMenuTriggerFor]="menu"
        class="menu-toggle"
        aria-label="Open navigation menu"
      >
        <mat-icon>menu</mat-icon>
      </button>
      <mat-menu #menu="matMenu">
        <!-- Student menu items -->
        @if (isStudent()) {
        <button mat-menu-item routerLink="/dashboard">
          <mat-icon>dashboard</mat-icon>
          Dashboard
        </button>
        <button mat-menu-item routerLink="/applications">
          <mat-icon>event_note</mat-icon>
          Applications
        </button>
        <button mat-menu-item routerLink="/documents">
          <mat-icon>folder</mat-icon>
          Documents
        </button>
        }
        <!-- Admin menu items removed (now in Sidebar) -->
        <button mat-menu-item [matMenuTriggerFor]="profileMenu">
          <mat-icon>account_circle</mat-icon>
          Profile
        </button>
      </mat-menu>
      <mat-menu #profileMenu="matMenu">
        @if (isStudent()) {
        <button mat-menu-item routerLink="/profile">
          <mat-icon>person</mat-icon>
          View Profile
        </button>
        }
        <button mat-menu-item routerLink="/change-password">
          <mat-icon>lock</mat-icon>
          Change Password
        </button>
        @if (isStudent()) {
        <button mat-menu-item routerLink="/settings">
          <mat-icon>settings</mat-icon>
          Settings
        </button>
        }
        <button mat-menu-item (click)="logout.emit()">
          <mat-icon>logout</mat-icon>
          Logout
        </button>
      </mat-menu>
      <div class="nav-tabs desktop-only">
        <!-- Student navigation tabs -->
        @if (isStudent()) {
        <a mat-button routerLink="/dashboard"
          ><mat-icon>dashboard</mat-icon><span>Dashboard</span></a
        >
        <a mat-button routerLink="/applications"
          ><mat-icon>event_note</mat-icon><span>Applications</span></a
        >
        <a mat-button routerLink="/documents"><mat-icon>folder</mat-icon><span>Documents</span></a>
        }
        <!-- Admin navigation tabs removed (now in Sidebar) -->
        <button mat-button [matMenuTriggerFor]="profileMenu" class="profile-btn">
          <mat-icon>account_circle</mat-icon><span>Profile</span><mat-icon>expand_more</mat-icon>
        </button>
      </div>
    </nav>
  `,
  styleUrls: ['./app-nav-menu.component.scss'],
})
export class AppNavMenuComponent {
  private authService = inject(AuthService);

  @Output() logout = new EventEmitter<void>();

  isAdmin(): boolean {
    const currentUser = this.authService.currentUserValue;
    return currentUser?.userRole === 'ADMIN';
  }

  isStudent(): boolean {
    const currentUser = this.authService.currentUserValue;
    return currentUser?.userRole === 'STUDENT';
  }
}
