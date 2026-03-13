import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [RouterModule, MatButtonModule, MatIconModule, NgClass],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  readonly stats = [
    { value: '5,000+', label: 'Students housed', icon: 'school', accentClass: 'accent-green' },
    { value: '200+', label: 'Rooms available', icon: 'meeting_room', accentClass: 'accent-cyan' },
    { value: '48h', label: 'Avg. response time', icon: 'schedule', accentClass: 'accent-blue' },
  ] as const;

  readonly features = [
    {
      icon: 'person_add',
      iconClass: 'icon-blue',
      title: 'Simple Applications',
      description: 'Submit your housing application in minutes with a guided step-by-step process and instant confirmation.',
    },
    {
      icon: 'meeting_room',
      iconClass: 'icon-cyan',
      title: 'Room Selection',
      description: 'Browse available rooms, compare options, and choose the accommodation that fits your needs and budget.',
    },
    {
      icon: 'security',
      iconClass: 'icon-purple',
      title: 'Secure & Reliable',
      description: 'Your personal data and documents are protected with enterprise-grade security throughout the process.',
    },
  ] as const;
}
