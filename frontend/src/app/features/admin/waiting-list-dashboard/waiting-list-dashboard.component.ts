import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { StagedAllocationService } from '../../../core/services/staged-allocation.service';
import { BuildingService, BuildingResponse } from '../../../core/services/building.service';
import { WaitingListEntry } from '../../../core/models/allocation.models';
import { AdminService } from '../../../core/services/admin.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-waiting-list-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './waiting-list-dashboard.component.html',
  styleUrls: ['./waiting-list-dashboard.component.css']
})
export class WaitingListDashboardComponent implements OnInit {
  filterForm: FormGroup;
  buildings: BuildingResponse[] = [];
  
  displayedColumns: string[] = ['position', 'studentName', 'priorityScore', 'preferredBuilding', 'applicationDate', 'actions'];
  dataSource: MatTableDataSource<WaitingListEntry>;
  
  isLoading = false;
  isRescoring = false;

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private fb: FormBuilder,
    private stagedAllocationService: StagedAllocationService,
    private buildingService: BuildingService,
    private adminService: AdminService, // For future detail view
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.dataSource = new MatTableDataSource<WaitingListEntry>([]);
    
    this.filterForm = this.fb.group({
      buildingId: ['']
    });
  }

  ngOnInit(): void {
    this.loadBuildings();
    this.loadWaitingList();

    this.filterForm.get('buildingId')?.valueChanges.subscribe(() => {
      this.loadWaitingList();
    });
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  loadBuildings(): void {
    this.buildingService.getAllBuildingsNoPagination().subscribe({
      next: (data) => this.buildings = data,
      error: (err) => console.error('Failed to load buildings', err)
    });
  }

  loadWaitingList(): void {
    this.isLoading = true;
    const buildingId = this.filterForm.get('buildingId')?.value;

    this.stagedAllocationService.getWaitingList(buildingId || undefined).subscribe({
      next: (entries) => {
        // Assign position based on score (assuming backend returns sorted, but we re-sort to be safe or rely on MatSort)
        // Backend usually returns sorted by Score DESC. 
        // We can add a temporary index property if needed for display.
        this.dataSource.data = entries;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load waiting list', err);
        this.isLoading = false;
        this.snackBar.open('Error loading waiting list', 'Close', { duration: 3000 });
      }
    });
  }

  rescoreList(): void {
    this.isRescoring = true;
    this.stagedAllocationService.rescoreWaitingList().subscribe({
      next: (res) => {
        this.snackBar.open(`Rescored ${res.rescored} applications.`, 'Close', { duration: 3000 });
        this.isRescoring = false;
        this.loadWaitingList(); // Refresh
      },
      error: (err) => {
        console.error('Rescore failed', err);
        this.isRescoring = false;
        this.snackBar.open('Failed to rescore waiting list', 'Close', { duration: 3000 });
      }
    });
  }

  // Helper for "Allocate Now"
  openAllocation(entry: WaitingListEntry): void {
     // Navigate to a detail view or open dialog?
     // For now, let's just show a not implemented or navigate to application review
     // Ideally, we would open the AllocationChoiceDialog here directly if we had the full ApplicationResponse.
     // But WaitingListEntry is a summary.
     // So let's navigate to application review with a filter.
     this.router.navigate(['/admin/applications'], { queryParams: { applicationCode: entry.applicationCode } });
  }
}
