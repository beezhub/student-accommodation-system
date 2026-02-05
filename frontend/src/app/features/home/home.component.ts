import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [RouterModule, MatCardModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  readonly features = [
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
  ] as const;
}
