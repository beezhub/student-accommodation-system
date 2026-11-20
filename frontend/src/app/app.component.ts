import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';

import { AuthService } from './core/services/auth.service';
import { IconService } from './core/services/icon.service';
import { User } from './core/models/user.model';
import { Subscription } from 'rxjs';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { AppNavMenuComponent } from './shared/app-nav-menu.component';
import { BrandLogoComponent } from './shared/components/brand-logo/brand-logo.component';

/** Full-screen pages that draw their own header, so the app navbar stays hidden. */
const TOOLBAR_HIDDEN_ROUTES = [
  '/home',
  '/login',
  '/signup',
  '/verify-email',
  '/email-verification-required',
  '/forgot-password',
  '/reset-password',
  '/profile-setup',
  '/document-upload',
  '/application-submission',
  '/application-review',
  '/application-confirmation',
  '/privacy',
];

/** True when `url` is one of those pages, with or without a query string. */
export function hidesToolbar(url: string): boolean {
  return (
    url.startsWith('/buildings') ||
    TOOLBAR_HIDDEN_ROUTES.some((route) => url === route || url.startsWith(route + '?'))
  );
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: true,
  imports: [
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatSidenavModule,
    MatListModule,
    AppNavMenuComponent,
    BrandLogoComponent,
  ],
  providers: [],
})
export class AppComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private location = inject(Location);
  private breakpointObserver = inject(BreakpointObserver);
  private iconService = inject(IconService);

  title = 'Student Accommodation Management';
  currentUser: User | null = null;
  private userSub!: Subscription;
  isMobile = false;
  isHomePage = false;
  isAdminRoute = false;

  constructor() {
    this.isMobile = this.breakpointObserver.isMatched(Breakpoints.Handset);
    // Initialize immediately so the toolbar is hidden on the first render
    this.updateRouteFlags(this.location.path() || '/');
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.updateRouteFlags(event.urlAfterRedirects);
      }
    });
  }

  private updateRouteFlags(url: string): void {
    this.isAdminRoute = url.startsWith('/admin');
    this.isHomePage = hidesToolbar(url);
  }

  ngOnInit() {
    this.userSub = this.authService.currentUser.subscribe((user) => {
      this.currentUser = user;
    });
  }

  ngOnDestroy() {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }

  logout() {
    this.authService.logout();
  }
}
