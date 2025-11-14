import { Component } from '@angular/core';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { IconService } from './core/services/icon.service';
import { User } from './core/models/user.model';
import { Subscription } from 'rxjs';
import { routes } from './app-routing.module';

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

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    MatSidenavModule,
    MatListModule,
    AppNavMenuComponent,
  ],
  providers: [],
})
export class AppComponent {
  title = 'Student Accommodation Management';
  currentUser: User | null = null;
  private userSub!: Subscription;
  isMobile = false;
  isHomePage = false;
  constructor(
    private authService: AuthService,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private iconService: IconService
  ) {
    this.isMobile = this.breakpointObserver.isMatched(Breakpoints.Handset);
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.isHomePage = event.urlAfterRedirects === '/home' || event.urlAfterRedirects === '/login';
      }
    });
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
