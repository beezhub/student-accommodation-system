import { Component } from '@angular/core';

import { RouterModule } from '@angular/router';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  standalone: true,
  imports: [RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatGridListModule],
})
export class HomeComponent {
  features = [
    {
      title: 'Easy Registration',
      description: 'Simple and quick registration process for students',
      icon: 'person_add',
    },
    {
      title: 'Room Selection',
      description: 'Choose from various accommodation options',
      icon: 'home',
    },
    {
      title: 'Secure Platform',
      description: 'Safe and secure platform for all transactions',
      icon: 'security',
    },
  ];
}
