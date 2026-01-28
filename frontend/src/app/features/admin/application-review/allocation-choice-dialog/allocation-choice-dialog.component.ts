import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { RoomService, RoomResponse } from '../../../../core/services/room.service';
import { BuildingService, BuildingResponse } from '../../../../core/services/building.service';
import { StagedAllocationService } from '../../../../core/services/staged-allocation.service';
import { SystemSettingsService } from '../../../../core/services/system-settings.service';
import { ApplicationService } from '../../../../core/services/application.service';
import { BatchAllocationProposal, StagedAllocation } from '../../../../core/models/allocation.models';

export interface AllocationChoiceDialogData {
  applicationId: number;
  studentId: number;
}

@Component({
  selector: 'app-allocation-choice-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatStepperModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './allocation-choice-dialog.component.html',
  styleUrls: ['./allocation-choice-dialog.component.css'],
})
export class AllocationChoiceDialogComponent implements OnInit {
  methodForm: FormGroup;
  autoAllocationEnabled = false;
  isLoading = false;
  
  // Auto Allocation State
  proposal: StagedAllocation | null = null;
  
  // Manual Allocation State
  searchForm: FormGroup;
  buildings: BuildingResponse[] = [];
  rooms: RoomResponse[] = [];
  displayedColumns: string[] = ['roomNumber', 'building', 'capacity', 'actions'];
  isAllocating = false;
  
  studentId: number | null = null;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AllocationChoiceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AllocationChoiceDialogData,
    private stagedAllocationService: StagedAllocationService,
    private systemSettingsService: SystemSettingsService,
    private snackBar: MatSnackBar,
    private roomService: RoomService,
    private buildingService: BuildingService,
    private applicationService: ApplicationService
  ) {
    this.methodForm = this.fb.group({
      method: ['manual', Validators.required],
    });

    this.searchForm = this.fb.group({
      buildingId: [''],
      roomNumber: ['']
    });
    
    // Initialize studentId from data
    if (data && data.studentId) {
      this.studentId = data.studentId;
    }
  }

  ngOnInit(): void {
    this.systemSettingsService.isAutoAllocationEnabled().subscribe((enabled) => {
      this.autoAllocationEnabled = enabled;
      if (!enabled) {
        this.methodForm.get('method')?.setValue('manual');
        this.methodForm.get('method')?.disable();
      }
    });
    this.loadBuildings();
    
    // If studentId is missing, try to fetch it from application details
    if (!this.studentId && this.data.applicationId) {
      this.isLoading = true;
      this.applicationService.getApplicationById(this.data.applicationId).subscribe({
        next: (app) => {
          if (app.studentId) {
            this.studentId = app.studentId;
            console.log('Fetched missing studentId:', this.studentId);
          } else {
            console.error('Fetched application but studentId is missing:', app);
            this.error = 'Could not determine student ID for this application.';
          }
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Failed to fetch application details:', err);
          this.error = 'Failed to load application details.';
          this.isLoading = false;
        }
      });
    }
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => this.buildings = data,
      error: (err) => console.error('Failed to load buildings', err)
    });
  }

  onMethodSubmit(): void {
    const method = this.methodForm.get('method')?.value;
    if (method === 'auto') {
      this.performAutoAllocation();
    } else {
      this.searchRooms();
    }
  }

  performAutoAllocation(): void {
    this.isLoading = true;
    const batchId = `AUTO-${new Date().getTime()}`;
    
    if (!this.studentId) {
      this.snackBar.open('Error: Student ID not available', 'Close', { duration: 3000 });
      this.isLoading = false;
      return;
    }

    this.stagedAllocationService
      .proposeAllocation([this.studentId], batchId)
      .subscribe({
        next: (response: BatchAllocationProposal) => {
          this.isLoading = false;
          if (response.proposals.length > 0) {
            this.proposal = response.proposals[0];
          } else {
             this.snackBar.open('No suitable room found for auto-allocation.', 'Close', { duration: 5000 });
          }
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Auto allocation failed:', error);
          this.snackBar.open('Auto allocation failed. Please try again.', 'Close', { duration: 3000 });
        },
      });
  }

  confirmAutoAllocation(): void {
    if (!this.proposal) return;
    
    this.isLoading = true;
    this.stagedAllocationService.confirmAllocation(this.proposal.id).subscribe({
      next: () => {
        this.isLoading = false;
        this.dialogRef.close({ action: 'allocated' });
        this.snackBar.open('Allocation confirmed!', 'Close', { duration: 3000 });
      },
      error: (error) => {
        this.isLoading = false;
        this.snackBar.open('Failed to confirm allocation.', 'Close', { duration: 3000 });
      }
    });
  }

  searchRooms(): void {
    this.isLoading = true;
    const filters = this.searchForm.value;
    this.roomService.searchRooms({
      buildingId: filters.buildingId || undefined,
      roomNumber: filters.roomNumber || undefined,
      onlyAvailable: true,
      page: 0,
      size: 50
    }).subscribe({
      next: (page) => {
        this.rooms = page.content;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to search rooms', err);
        this.isLoading = false;
        this.snackBar.open('Failed to load rooms.', 'Close', { duration: 3000 });
      }
    });
  }
  
  selectRoom(room: RoomResponse): void {
      console.log('Selecting room for manual allocation:', room);
      console.log('Dialog Data:', this.data);
      console.log('Using Student ID:', this.studentId);
      
      if (!this.studentId) {
        console.error('Missing studentId!');
        this.snackBar.open('Error: Missing student ID. Cannot allocate.', 'Close');
        return;
      }

      this.isAllocating = true;
      // We use overrideConstraints to force a manual allocation (bypass constraints if needed, but here we just mean manual selection)
      // If the backend requires a reason, we provide "Manual Selection"
      this.stagedAllocationService.overrideConstraints(
        this.studentId, 
        room.id, 
        'Manual Selection', 
        [] // No bypass constraints explicitly checked here yet, assuming admin knows what they are doing or backend validates
      ).subscribe({
        next: (stagedAllocation) => {
           // Once staged/proposed via override, we confirm it immediately
           this.stagedAllocationService.confirmAllocation(stagedAllocation.id).subscribe({
             next: () => {
               this.isAllocating = false;
               this.dialogRef.close({ action: 'allocated' });
               this.snackBar.open('Room assigned successfully!', 'Close', { duration: 3000 });
             },
             error: (err) => {
               this.isAllocating = false;
               console.error('Confirm failed', err);
               this.snackBar.open('Failed to confirm assignment.', 'Close', { duration: 3000 });
             }
           });
        },
        error: (err) => {
          this.isAllocating = false;
          console.error('Manual allocation failed', err);
          this.snackBar.open('Failed to assign room. It might be full or restricted.', 'Close', { duration: 3000 });
        }
      });
  }
}
