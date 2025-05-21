import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-application-confirmation',
  templateUrl: './application-confirmation.component.html',
  styleUrls: ['./application-confirmation.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class ApplicationConfirmationComponent implements OnInit {
  currentUser: any;
  currentDate: Date = new Date();

  constructor(
      private router: Router,
      private authService: AuthService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
  }

  goToDashboard() {
    this.router.navigate(['/dashboard']);
  }
}