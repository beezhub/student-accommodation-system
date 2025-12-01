import { Component, Output, EventEmitter } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';

@Component({
    selector: 'app-nav-menu',
    standalone: true,
    imports: [CommonModule, MatMenuModule, MatIconModule, MatButtonModule, RouterModule],
    template: `
    <nav class="nav-bar">
      <button mat-icon-button [matMenuTriggerFor]="menu" class="menu-toggle" aria-label="Open navigation menu">
        <mat-icon>menu</mat-icon>
      </button>
      <mat-menu #menu="matMenu">
        <!-- Student menu items -->
        <ng-container *ngIf="isStudent()">
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
          <button mat-menu-item routerLink="/rooms">
            <mat-icon>hotel</mat-icon>
            Browse Rooms
          </button>
        </ng-container>
        <!-- Admin menu items -->
        <ng-container *ngIf="isAdmin()">
          <button mat-menu-item routerLink="/admin">
            <mat-icon>dashboard</mat-icon>
            Admin Dashboard
          </button>
          <button mat-menu-item routerLink="/admin/applications">
            <mat-icon>assignment</mat-icon>
            Applications
          </button>
          <button mat-menu-item routerLink="/admin/documents">
            <mat-icon>verified</mat-icon>
            Documents
          </button>
          <button mat-menu-item routerLink="/admin/users">
            <mat-icon>people</mat-icon>
            Users
          </button>
          <button mat-menu-item routerLink="/admin/buildings">
            <mat-icon>apartment</mat-icon>
            Buildings
          </button>
          <button mat-menu-item routerLink="/admin/rooms">
            <mat-icon>meeting_room</mat-icon>
            Rooms
          </button>
          <button mat-menu-item routerLink="/admin/room-allocations">
            <mat-icon>assignment_ind</mat-icon>
            Room Allocations
          </button>
        </ng-container>
        <button mat-menu-item [matMenuTriggerFor]="profileMenu">
          <mat-icon>account_circle</mat-icon>
          Profile
        </button>
      </mat-menu>
      <mat-menu #profileMenu="matMenu">
        <button mat-menu-item routerLink="/profile" *ngIf="isStudent()">
          <mat-icon>person</mat-icon>
          View Profile
        </button>
        <button mat-menu-item routerLink="/change-password">
          <mat-icon>lock</mat-icon>
          Change Password
        </button>
        <button mat-menu-item routerLink="/settings" *ngIf="isStudent()">
          <mat-icon>settings</mat-icon>
          Settings
        </button>
        <button mat-menu-item (click)="logout.emit()">
          <mat-icon>logout</mat-icon>
          Logout
        </button>
      </mat-menu>
      <div class="nav-tabs desktop-only">
        <!-- Student navigation tabs -->
        <ng-container *ngIf="isStudent()">
          <a mat-button routerLink="/dashboard"><mat-icon>dashboard</mat-icon> Dashboard</a>
          <a mat-button routerLink="/applications"><mat-icon>event_note</mat-icon> Applications</a>
          <a mat-button routerLink="/documents"><mat-icon>folder</mat-icon> Documents</a>
          <a mat-button routerLink="/rooms"><mat-icon>hotel</mat-icon> Browse Rooms</a>
        </ng-container>
        <!-- Admin navigation tabs -->
        <ng-container *ngIf="isAdmin()">
          <a mat-button routerLink="/admin"><mat-icon>dashboard</mat-icon> Dashboard</a>
          <a mat-button routerLink="/admin/applications"><mat-icon>assignment</mat-icon> Applications</a>
          <a mat-button routerLink="/admin/buildings"><mat-icon>apartment</mat-icon> Buildings</a>
          <a mat-button routerLink="/admin/rooms"><mat-icon>meeting_room</mat-icon> Rooms</a>
          <a mat-button routerLink="/admin/room-allocations"><mat-icon>assignment_ind</mat-icon> Allocations</a>
        </ng-container>
        <button mat-button [matMenuTriggerFor]="profileMenu" class="profile-btn">
          <mat-icon>account_circle</mat-icon> Profile <mat-icon>expand_more</mat-icon>
        </button>
      </div>
    </nav>
  `,
    styleUrls: ['./app-nav-menu.component.css']
})
export class AppNavMenuComponent {
    @Output() logout = new EventEmitter<void>();

    constructor(private authService: AuthService) {}

    isAdmin(): boolean {
      const currentUser = this.authService.currentUserValue;
      return currentUser?.userRole === 'ADMIN';
    }

    isStudent(): boolean {
      const currentUser = this.authService.currentUserValue;
      return currentUser?.userRole === 'STUDENT';
    }
}
