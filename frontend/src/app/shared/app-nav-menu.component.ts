import { Component, Output, EventEmitter } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

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
        <button mat-menu-item [matMenuTriggerFor]="profileMenu">
          <mat-icon>account_circle</mat-icon>
          Profile
        </button>
      </mat-menu>
      <mat-menu #profileMenu="matMenu">
        <button mat-menu-item routerLink="/profile">
          <mat-icon>person</mat-icon>
          View Profile
        </button>
        <button mat-menu-item routerLink="/settings">
          <mat-icon>settings</mat-icon>
          Settings
        </button>
        <button mat-menu-item (click)="logout.emit()">
          <mat-icon>logout</mat-icon>
          Logout
        </button>
      </mat-menu>
      <div class="nav-tabs desktop-only">
        <a mat-button routerLink="/dashboard"><mat-icon>dashboard</mat-icon> Dashboard</a>
        <a mat-button routerLink="/applications"><mat-icon>event_note</mat-icon> Applications</a>
        <a mat-button routerLink="/documents"><mat-icon>folder</mat-icon> Documents</a>
        <button mat-button [matMenuTriggerFor]="profileMenu" class="profile-btn">
          <mat-icon>account_circle</mat-icon> tee <mat-icon>expand_more</mat-icon>
        </button>
      </div>
    </nav>
  `,
    styleUrls: ['./app-nav-menu.component.css']
})
export class AppNavMenuComponent {
    @Output() logout = new EventEmitter<void>();
}
