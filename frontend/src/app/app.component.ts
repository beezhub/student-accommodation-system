import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { User } from './core/models/user.model';
import { Subscription } from 'rxjs';
import { routes } from './app-routing.module';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: true,
  imports: [RouterModule, CommonModule],
  providers: []
})
export class AppComponent {
  title = 'Student Accommodation Management';
  currentUser: User | null = null;
  private userSub!: Subscription;


  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.userSub = this.authService.currentUser.subscribe(user => {
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