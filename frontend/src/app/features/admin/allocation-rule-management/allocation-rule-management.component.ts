import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { AllocationRuleService } from '../../../core/services/allocation-rule.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { AllocationRule } from '../../../core/models/allocation.models';
import { AllocationRuleDialogComponent } from './allocation-rule-dialog/allocation-rule-dialog.component';

@Component({
  selector: 'app-allocation-rule-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatDialogModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatDividerModule,
  ],
  templateUrl: './allocation-rule-management.component.html',
  styleUrls: ['./allocation-rule-management.component.scss'],
})
export class AllocationRuleManagementComponent implements OnInit {
  buildingForm: FormGroup;
  buildings: BuildingResponse[] = [];
  rules: AllocationRule[] = [];
  isLoading = false;
  displayedColumns: string[] = ['priority', 'ruleName', 'weights', 'active', 'actions'];

  constructor(
    private fb: FormBuilder,
    private allocationRuleService: AllocationRuleService,
    private buildingService: BuildingService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private route: ActivatedRoute
  ) {
    this.buildingForm = this.fb.group({
      buildingId: [''],
    });
  }

  ngOnInit(): void {
    this.loadBuildings();

    // Listen for building changes to load rules
    this.buildingForm.get('buildingId')?.valueChanges.subscribe((buildingId) => {
      if (buildingId !== '') {
        this.loadRules(buildingId);
      } else {
        this.rules = [];
      }
    });

    // Check for buildingId in query params (e.g., from building management page)
    this.route.queryParams.subscribe((params) => {
      if (params['buildingId']) {
        const buildingId = parseInt(params['buildingId'], 10);
        if (!isNaN(buildingId)) {
          // Wait for buildings to load, then set the form value
          setTimeout(() => {
            this.buildingForm.patchValue({ buildingId: buildingId });
          }, 500);
        }
      }
    });
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => (this.buildings = data),
      error: (err) => {
        console.error('Failed to load buildings', err);
        this.snackBar.open('Failed to load buildings', 'Close', { duration: 3000 });
      },
    });
  }

  loadRules(buildingId: number | null): void {
    this.isLoading = true;
    this.allocationRuleService.getRulesForBuilding(buildingId).subscribe({
      next: (rules) => {
        this.rules = rules.sort((a, b) => b.priority - a.priority); // Sort by priority desc (higher = more important)
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load rules', err);
        this.isLoading = false;
        this.snackBar.open('Failed to load rules', 'Close', { duration: 3000 });
      },
    });
  }

  openRuleDialog(rule?: AllocationRule): void {
    const buildingId = this.buildingForm.get('buildingId')?.value;
    if (buildingId === '' && !rule) {
      this.snackBar.open('Please select a building first', 'Close', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(AllocationRuleDialogComponent, {
      width: '650px',
      maxHeight: '90vh',
      data: {
        rule,
        buildingId: buildingId,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadRules(buildingId); // Reload rules
      }
    });
  }

  toggleRuleActive(rule: AllocationRule): void {
    this.allocationRuleService.activateRule(rule.id).subscribe({
      next: (updated) => {
        rule.active = updated.active;
        this.snackBar.open(`Rule ${updated.active ? 'activated' : 'deactivated'}`, 'Close', {
          duration: 2000,
        });
      },
      error: (err) => {
        console.error('Failed to update rule status', err);
        this.snackBar.open('Failed to update status', 'Close', { duration: 3000 });
        // Revert UI change if needed (but since we didn't optimistic update, it's fine)
      },
    });
  }

  deleteRule(rule: AllocationRule): void {
    if (confirm(`Are you sure you want to delete rule "${rule.ruleName}"?`)) {
      this.allocationRuleService.deleteRule(rule.id).subscribe({
        next: () => {
          this.snackBar.open('Rule deleted', 'Close', { duration: 3000 });
          const buildingId = this.buildingForm.get('buildingId')?.value;
          if (buildingId) this.loadRules(buildingId);
        },
        error: (err) => {
          console.error('Failed to delete rule', err);
          this.snackBar.open('Failed to delete rule', 'Close', { duration: 3000 });
        },
      });
    }
  }
}
