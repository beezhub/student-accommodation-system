import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// Material imports
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-system-settings',
  templateUrl: './system-settings.component.html',
  styleUrls: ['./system-settings.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
  ],
})
export class SystemSettingsComponent {
  settingsCategories = [
    {
      title: 'Document Types',
      description: 'Configure document types required for student applications',
      icon: 'description',
      route: '/admin/document-types',
      class: 'documents',
    },
    {
      title: 'Institutions',
      description: 'Manage partner educational institutions',
      icon: 'school',
      route: '/admin/institutions',
      class: 'institutions',
    },
    {
      title: 'Year of Study',
      description: 'Configure available years of study',
      icon: 'calendar_today',
      route: '/admin/year-of-study',
      class: 'years',
    },
  ];
}
