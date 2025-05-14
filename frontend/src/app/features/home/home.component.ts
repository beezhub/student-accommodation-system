import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  standalone: true,
  imports: [CommonModule, RouterModule]
})
export class HomeComponent {
  features = [
    {
      title: 'Easy Registration',
      description: 'Simple and quick registration process for students',
      icon: 'bi-person-plus'
    },
    {
      title: 'Room Selection',
      description: 'Choose from various accommodation options',
      icon: 'bi-house-door'
    },
    {
      title: 'Secure Platform',
      description: 'Safe and secure platform for all transactions',
      icon: 'bi-shield-check'
    }
  ];
}