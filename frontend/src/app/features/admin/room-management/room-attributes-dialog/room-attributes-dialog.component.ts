import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSliderModule } from '@angular/material/slider';

import {
  RoomAttributesService,
  RoomAttributesResponse,
  RoomAttributesRequest,
  GenderRestriction,
} from '../../../../core/services/room-attributes.service';
import { YearOfStudyService } from '../../../../core/services/year-of-study.service';

export interface RoomAttributesDialogData {
  roomId: number;
  roomNumber: string;
  buildingName: string;
}

@Component({
  selector: 'app-room-attributes-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSliderModule,
  ],
  templateUrl: './room-attributes-dialog.component.html',
  styleUrls: ['./room-attributes-dialog.component.css'],
})
export class RoomAttributesDialogComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject<MatDialogRef<RoomAttributesDialogComponent>>(MatDialogRef);
  data = inject<RoomAttributesDialogData>(MAT_DIALOG_DATA);
  private roomAttributesService = inject(RoomAttributesService);
  private yearOfStudyService = inject(YearOfStudyService);
  private snackBar = inject(MatSnackBar);

  attributesForm: FormGroup;
  isLoading = true;
  isSaving = false;
  currentAttributes: RoomAttributesResponse | null = null;

  genderOptions: { value: GenderRestriction; label: string; icon: string }[] = [
    { value: 'ANY', label: 'No Restriction (Any Gender)', icon: 'people' },
    { value: 'FEMALE_ONLY', label: 'Female Only', icon: 'female' },
    { value: 'MALE_ONLY', label: 'Male Only', icon: 'male' },
  ];

  yearOptions: { value: number; label: string }[] = [];
  isLoadingYears = false;
  unparseableYears: string[] = []; // Years that couldn't be converted to numbers

  constructor() {
    this.attributesForm = this.fb.group({
      accessibilityCompliant: [false],
      genderRestriction: ['ANY'],
      minYearOfStudy: [null],
      maxYearOfStudy: [null],
    });
  }

  ngOnInit(): void {
    this.loadYearOptions();
    this.loadAttributes();
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

  loadAttributes(): void {
    this.isLoading = true;
    this.roomAttributesService.getAttributesByRoomId(this.data.roomId).subscribe({
      next: (attributes) => {
        this.currentAttributes = attributes;
        this.attributesForm.patchValue({
          accessibilityCompliant: attributes.accessibilityCompliant || false,
          genderRestriction: attributes.genderRestriction || 'ANY',
          minYearOfStudy: attributes.minYearOfStudy || null,
          maxYearOfStudy: attributes.maxYearOfStudy || null,
        });
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load attributes', err);
        this.isLoading = false;
        // Use defaults on error
        this.attributesForm.patchValue({
          accessibilityCompliant: false,
          genderRestriction: 'ANY',
          minYearOfStudy: null,
          maxYearOfStudy: null,
        });
      },
    });
  }

  onSubmit(): void {
    if (this.attributesForm.invalid) return;

    // Validate year range
    const formValue = this.attributesForm.value;
    if (
      formValue.minYearOfStudy &&
      formValue.maxYearOfStudy &&
      formValue.minYearOfStudy > formValue.maxYearOfStudy
    ) {
      this.snackBar.open('Minimum year cannot be greater than maximum year', 'Close', {
        duration: 3000,
      });
      return;
    }

    this.isSaving = true;

    const request: RoomAttributesRequest = {
      roomId: this.data.roomId,
      accessibilityCompliant: formValue.accessibilityCompliant,
      genderRestriction: formValue.genderRestriction,
      minYearOfStudy: formValue.minYearOfStudy || null,
      maxYearOfStudy: formValue.maxYearOfStudy || null,
    };

    this.roomAttributesService.saveAttributes(request).subscribe({
      next: (saved) => {
        this.snackBar.open('Room attributes saved successfully', 'Close', { duration: 3000 });
        this.dialogRef.close(saved);
        this.isSaving = false;
      },
      error: (err) => {
        console.error('Failed to save attributes', err);
        this.snackBar.open('Failed to save attributes', 'Close', { duration: 3000 });
        this.isSaving = false;
      },
    });
  }

  clearYearRestriction(): void {
    this.attributesForm.patchValue({ minYearOfStudy: null, maxYearOfStudy: null });
  }

  getYearLabel(value: number | null): string {
    if (value === null) return '';
    const year = this.yearOptions.find((y) => y.value === value);
    return year ? year.label : `Year ${value}`;
  }
}
