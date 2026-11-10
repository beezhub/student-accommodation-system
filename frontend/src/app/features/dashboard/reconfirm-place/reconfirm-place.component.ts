import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  RoomAllocationResponse,
  RoomAllocationService,
} from '../../../core/services/room-allocation.service';
import { AllocationStatus } from '../../../core/models/status.model';

/**
 * "Still coming?" on the dashboard (GH-496). A place accepted long before move-in is asked to
 * reconfirm about four weeks ahead; one not reconfirmed about two weeks ahead goes to the next
 * student on the waiting list. Shows nothing for any other allocation.
 */
@Component({
  selector: 'app-reconfirm-place',
  templateUrl: './reconfirm-place.component.html',
  styleUrls: ['./reconfirm-place.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatIconModule],
})
export class ReconfirmPlaceComponent {
  private readonly roomAllocationService = inject(RoomAllocationService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) allocation!: RoomAllocationResponse;
  @Output() reconfirmed = new EventEmitter<RoomAllocationResponse>();

  processing = false;

  /** Asked and not yet answered. */
  get awaitingAnswer(): boolean {
    return (
      this.allocation.status === AllocationStatus.ACCEPTED &&
      !!this.allocation.reconfirmRequestedAt &&
      !this.allocation.reconfirmedAt
    );
  }

  /** Confirmed and still waiting to move in. */
  get confirmed(): boolean {
    return this.allocation.status === AllocationStatus.ACCEPTED && !!this.allocation.reconfirmedAt;
  }

  confirm(): void {
    this.processing = true;
    this.roomAllocationService.reconfirm(this.allocation.id).subscribe({
      next: (updated) => {
        this.processing = false;
        this.reconfirmed.emit(updated);
        this.cdr.markForCheck();
      },
      error: () => {
        this.processing = false;
        this.cdr.markForCheck();
      },
    });
  }
}
