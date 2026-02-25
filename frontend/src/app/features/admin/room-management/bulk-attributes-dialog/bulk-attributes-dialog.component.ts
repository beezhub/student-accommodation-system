import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';

import {
  RoomAttributesService,
  RoomAttributesRequest,
  GenderRestriction,
} from '../../../../core/services/room-attributes.service';
import { YearOfStudyService } from '../../../../core/services/year-of-study.service';

export interface BulkAttributesDialogData {
  rooms: { id: number; roomNumber: string; buildingName: string }[];
}

@Component({
  selector: 'app-bulk-attributes-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
  ],
  templateUrl: './bulk-attributes-dialog.component.html',
  styleUrls: ['./bulk-attributes-dialog.component.css'],
})
export class BulkAttributesDialogComponent implements OnInit {
  attributesForm: FormGroup;
  isSaving = false;

  // Toggles for which fields to update
  updateGender = false;
  updateAccessibility = false;
  updateYearRange = false;

  genderOptions: { value: GenderRestriction; label: string; icon: string }[] = [
    { value: 'ANY', label: 'No Restriction', icon: 'people' },
    { value: 'FEMALE_ONLY', label: 'Female Only', icon: 'female' },
    { value: 'MALE_ONLY', label: 'Male Only', icon: 'male' },
  ];

  yearOptions: { value: number; label: string }[] = [];
  isLoadingYears = false;
  unparseableYears: string[] = []; // Years that couldn't be converted to numbers

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<BulkAttributesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BulkAttributesDialogData,
    private roomAttributesService: RoomAttributesService,
    private yearOfStudyService: YearOfStudyService,
    private snackBar: MatSnackBar
  ) {
    this.attributesForm = this.fb.group({
      genderRestriction: ['ANY'],
      accessibilityCompliant: [false],
      minYearOfStudy: [null],
      maxYearOfStudy: [null],
    });
  }

  ngOnInit(): void {
    this.loadYearOptions();
  }

  loadYearOptions(): void {
    this.isLoadingYears = true;
    this.unparseableYears = [];
    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (years) => {
        const parsedYears: { value: number; label: string }[] = [];

        // Parse year strings to numbers (handles both numeric and word formats)
        years.forEach((y) => {
          const parsed = this.parseYearOfStudy(y.yearOfStudy);
          if (parsed !== null && parsed > 0) {
            parsedYears.push({ value: parsed, label: y.yearOfStudy });
          } else {
            // Track years that couldn't be parsed
            this.unparseableYears.push(y.yearOfStudy);
          }
        });

        // Remove duplicates by value and sort
        const uniqueMap = new Map<number, { value: number; label: string }>();
        parsedYears.forEach((y) => {
          if (!uniqueMap.has(y.value)) {
            uniqueMap.set(y.value, y);
          }
        });
        this.yearOptions = Array.from(uniqueMap.values()).sort((a, b) => a.value - b.value);
        this.isLoadingYears = false;
      },
      error: (err) => {
        console.error('Failed to load year options', err);
        // Fallback to default values if service fails
        this.yearOptions = [
          { value: 1, label: '1st Year' },
          { value: 2, label: '2nd Year' },
          { value: 3, label: '3rd Year' },
          { value: 4, label: '4th Year' },
          { value: 5, label: '5th Year' },
          { value: 6, label: '6th Year' },
          { value: 7, label: '7th Year' },
        ];
        this.isLoadingYears = false;
      },
    });
  }

  /**
   * Parses year of study string to a number.
   * Handles formats like: "1", "First Year", "1st Year", "Year 1", "first", etc.
   */
  private parseYearOfStudy(value: string): number | null {
    if (!value) return null;

    const normalized = value.toLowerCase().trim();

    // Try direct numeric parse first
    const directParse = parseInt(normalized, 10);
    if (!isNaN(directParse)) return directParse;

    // Word to number mapping
    const wordMap: { [key: string]: number } = {
      first: 1,
      one: 1,
      '1st': 1,
      second: 2,
      two: 2,
      '2nd': 2,
      third: 3,
      three: 3,
      '3rd': 3,
      fourth: 4,
      four: 4,
      '4th': 4,
      fifth: 5,
      five: 5,
      '5th': 5,
      sixth: 6,
      six: 6,
      '6th': 6,
      seventh: 7,
      seven: 7,
      '7th': 7,
      eighth: 8,
      eight: 8,
      '8th': 8,
      ninth: 9,
      nine: 9,
      '9th': 9,
      tenth: 10,
      ten: 10,
      '10th': 10,
    };

    // Check each word in the string against the map
    for (const [word, num] of Object.entries(wordMap)) {
      if (normalized.includes(word)) {
        return num;
      }
    }

    // Try to extract number from string (e.g., "Year 3" -> 3)
    const numberMatch = normalized.match(/\d+/);
    if (numberMatch) {
      return parseInt(numberMatch[0], 10);
    }

    return null;
  }

  onSubmit(): void {
    // Validate year range
    const formValue = this.attributesForm.value;
    if (
      this.updateYearRange &&
      formValue.minYearOfStudy &&
      formValue.maxYearOfStudy &&
      formValue.minYearOfStudy > formValue.maxYearOfStudy
    ) {
      this.snackBar.open('Minimum year cannot be greater than maximum year', 'Close', {
        duration: 3000,
      });
      return;
    }

    // Check if at least one field is being updated
    if (!this.updateGender && !this.updateAccessibility && !this.updateYearRange) {
      this.snackBar.open('Please select at least one attribute to update', 'Close', {
        duration: 3000,
      });
      return;
    }

    this.isSaving = true;

    // Build requests for each room
    const requests: RoomAttributesRequest[] = this.data.rooms.map((room) => {
      const request: RoomAttributesRequest = {
        roomId: room.id,
        genderRestriction: this.updateGender ? formValue.genderRestriction : undefined,
        accessibilityCompliant: this.updateAccessibility
          ? formValue.accessibilityCompliant
          : undefined,
        minYearOfStudy: this.updateYearRange ? formValue.minYearOfStudy || null : undefined,
        maxYearOfStudy: this.updateYearRange ? formValue.maxYearOfStudy || null : undefined,
      };
      return request;
    });

    this.roomAttributesService.bulkSaveAttributes(requests).subscribe({
      next: (saved) => {
        this.snackBar.open(`Updated attributes for ${saved.length} rooms`, 'Close', {
          duration: 3000,
        });
        this.dialogRef.close(true);
        this.isSaving = false;
      },
      error: (err) => {
        console.error('Failed to bulk update attributes', err);
        this.snackBar.open('Failed to update attributes', 'Close', { duration: 3000 });
        this.isSaving = false;
      },
    });
  }

  clearYearRestriction(): void {
    this.attributesForm.patchValue({ minYearOfStudy: null, maxYearOfStudy: null });
  }

  getSelectedCount(): number {
    return this.data.rooms.length;
  }
}
