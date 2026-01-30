import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SystemSettingsService } from '../../../core/services/system-settings.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-system-settings',
  templateUrl: './system-settings.component.html',
  styleUrls: ['./system-settings.component.css'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatSnackBarModule
  ],
})
export class SystemSettingsComponent implements OnInit {
  private readonly systemSettingsService = inject(SystemSettingsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  autoAllocationEnabled = false;
  loading = false;

  settingsCategories = [
    {
      title: 'Accommodation Types',
      description: 'Manage accommodation type options',
      icon: 'hotel',
      route: '/admin/accommodation-types',
      class: 'accommodation-types',
    },
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

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.systemSettingsService.getSettings().subscribe({
      next: (settings) => {
        this.autoAllocationEnabled = settings['auto_allocation_enabled'] === 'true';
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading settings', err);
        this.loading = false;
        this.cdr.markForCheck();
        this.snackBar.open('Failed to load system settings', 'Close', { duration: 3000 });
      }
    });
  }

  toggleAutoAllocation(): void {
    const newValue = (!this.autoAllocationEnabled).toString();
    // Optimistic update
    this.autoAllocationEnabled = !this.autoAllocationEnabled;
    this.cdr.markForCheck();

    this.systemSettingsService.updateSetting('auto_allocation_enabled', newValue).subscribe({
      next: () => {
        this.snackBar.open('Setting updated successfully', 'Close', { duration: 2000 });
      },
      error: (err) => {
        console.error('Error updating setting', err);
        // Revert on error
        this.autoAllocationEnabled = !this.autoAllocationEnabled;
        this.cdr.markForCheck();
        this.snackBar.open('Failed to update setting', 'Close', { duration: 3000 });
      }
    });
  }
}
