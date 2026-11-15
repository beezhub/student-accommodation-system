import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
  output,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { RoomAvailabilityResponse } from '../../../../../core/services/room-allocation.service';
import { RoomResponse, RoomService } from '../../../../../core/services/room.service';
import { AllocationStatusMessageComponent } from '../allocation-status-message/allocation-status-message.component';

/**
 * The room picker of the allocate dialog, with the availability of the chosen room under it.
 * Binds to the dialog's form, which must have a `roomId` control. The dialog runs the availability
 * check when a room is picked and passes the result back in.
 */
@Component({
  selector: 'app-allocation-room-field',
  templateUrl: './allocation-room-field.component.html',
  styleUrl: './allocation-room-field.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    AllocationStatusMessageComponent,
  ],
  // The field and its status line sit in the dialog's form column as if they were its own.
  host: { style: 'display: contents' },
})
export class AllocationRoomFieldComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly roomService = inject(RoomService);
  private readonly snackBar = inject(MatSnackBar);

  readonly group = input.required<FormGroup>();
  /** The last availability check of the chosen room; null until one has run. */
  readonly availability = input<RoomAvailabilityResponse | null>(null);
  /** The admin picked a room. */
  readonly roomSelected = output<void>();

  /** Options fetched per search. Small on purpose: these feed a dropdown, not a report. */
  readonly optionPageSize = 25;
  readonly roomSearch = new FormControl('');
  availableRooms: RoomResponse[] = [];
  moreRoomsExist = false;

  ngOnInit(): void {
    this.loadAvailableRooms();

    this.roomSearch.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((term) => this.loadAvailableRooms(term ?? ''));
  }

  /**
   * One page of rooms, narrowed by what the admin types.
   *
   * <p>Both dropdowns used to ask for 1000 rows up front (GH-157). That is a large payload for a
   * select, and it truncates rather than pages: room 1001 simply could not be chosen. Searching
   * server-side keeps the payload small and makes every room reachable.
   */
  loadAvailableRooms(roomNumber?: string): void {
    this.roomService
      .searchRooms({ page: 0, size: this.optionPageSize, roomNumber: roomNumber || undefined })
      .subscribe({
        next: (response) => {
          this.availableRooms = response.content.filter(
            (room) => room.status === 'AVAILABLE' || room.status === 'OCCUPIED'
          );
          this.moreRoomsExist = response.totalElements > response.content.length;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Failed to load rooms:', error);
          this.snackBar.open('Failed to load available rooms', 'Close', { duration: 3000 });
        },
      });
  }
}
