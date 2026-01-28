import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSliderModule } from '@angular/material/slider';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AllocationRuleService } from '../../../../core/services/allocation-rule.service';
import { AllocationRule } from '../../../../core/models/allocation.models';

export interface AllocationRuleDialogData {
  rule?: AllocationRule;
  buildingId: number;
}

@Component({
  selector: 'app-allocation-rule-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSliderModule
  ],
  templateUrl: './allocation-rule-dialog.component.html',
  styleUrls: ['./allocation-rule-dialog.component.css'],
})
export class AllocationRuleDialogComponent implements OnInit {
  ruleForm: FormGroup;
  isEditMode = false;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AllocationRuleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AllocationRuleDialogData,
    private allocationRuleService: AllocationRuleService,
    private snackBar: MatSnackBar
  ) {
    this.isEditMode = !!data.rule;
    
    this.ruleForm = this.fb.group({
      ruleName: [data.rule?.ruleName || '', [Validators.required, Validators.maxLength(100)]],
      priority: [data.rule?.priority || 1, [Validators.required, Validators.min(1)]],
      preferenceMatchWeight: [data.rule?.preferenceMatchWeight || 50, [Validators.required, Validators.min(0), Validators.max(100)]],
      desirabilityWeight: [data.rule?.desirabilityWeight || 30, [Validators.required, Validators.min(0), Validators.max(100)]],
      accessibilityWeight: [data.rule?.accessibilityWeight || 20, [Validators.required, Validators.min(0), Validators.max(100)]],
      active: [data.rule?.active ?? true]
    });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.ruleForm.invalid) return;

    this.isSaving = true;
    const formValue = this.ruleForm.value;
    
    // Construct the rule object
    const ruleData: Partial<AllocationRule> = {
      ...formValue,
      buildingId: this.data.buildingId
    };

    if (this.isEditMode && this.data.rule) {
      this.allocationRuleService.updateRule(this.data.rule.id, ruleData).subscribe({
        next: (updatedRule) => {
          this.snackBar.open('Rule updated successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(updatedRule);
          this.isSaving = false;
        },
        error: (err) => {
          console.error('Failed to update rule', err);
          this.snackBar.open('Failed to update rule', 'Close', { duration: 3000 });
          this.isSaving = false;
        }
      });
    } else {
      this.allocationRuleService.createRule(ruleData).subscribe({
        next: (newRule) => {
          this.snackBar.open('Rule created successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(newRule);
          this.isSaving = false;
        },
        error: (err) => {
          console.error('Failed to create rule', err);
          this.snackBar.open('Failed to create rule', 'Close', { duration: 3000 });
          this.isSaving = false;
        }
      });
    }
  }
}
