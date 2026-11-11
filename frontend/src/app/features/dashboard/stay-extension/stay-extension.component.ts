import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  RoomAllocationResponse,
  RoomAllocationService,
  StayExtensionResponse,
} from '../../../core/services/room-allocation.service';
import { AllocationStatus } from '../../../core/models/status.model';

/**
 * "Stay for Semester 2" on the dashboard (GH-498). Shown on an accepted place or the room the
 * student lives in: the button when the bed is free for the next intake and the documents are in
 * order, otherwise a link to apply for that intake. If the bed was taken since the page loaded, the
 * tap is refused and the same link is shown.
 */
@Component({
  selector: 'app-stay-extension',
  templateUrl: './stay-extension.component.html',
  styleUrls: ['./stay-extension.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterModule, MatButtonModule, MatIconModule],
})
export class StayExtensionComponent implements OnChanges {
  private readonly roomAllocationService = inject(RoomAllocationService);
  private readonly cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) allocation!: RoomAllocationResponse;
  @Output() extended = new EventEmitter<RoomAllocationResponse>();

  option: StayExtensionResponse | null = null;
  processing = false;
  /** Why the tap was refused, when it was. */
  refusal: string | null = null;
  /** The intake just stayed on for, to confirm it. */
  stayedFor: { name: string; until: string } | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    const previous = changes['allocation']?.previousValue as RoomAllocationResponse | undefined;
    if (previous?.id !== this.allocation.id) {
      this.stayedFor = null;
    }
    this.load();
  }

  stay(): void {
    const option = this.option;
    if (!option?.allowed || !option.nextIntakeName) return;
    this.processing = true;
    this.refusal = null;
    this.roomAllocationService.extendStay(this.allocation.id).subscribe({
      next: (updated) => {
        this.processing = false;
        this.stayedFor = { name: option.nextIntakeName ?? '', until: option.nextStayEndDate ?? '' };
        this.option = null;
        this.extended.emit(updated);
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => {
        this.processing = false;
        this.option = { ...option, allowed: false };
        const body = error.error as { message?: string } | null;
        this.refusal =
          body?.message ?? 'This room is no longer free for ' + option.nextIntakeName + '.';
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    const status = this.allocation.status;
    if (status !== AllocationStatus.ACTIVE && status !== AllocationStatus.ACCEPTED) {
      this.option = null;
      return;
    }
    this.roomAllocationService.getStayExtension(this.allocation.id).subscribe({
      next: (option) => {
        this.option = option;
        this.cdr.markForCheck();
      },
      error: () => {
        this.option = null;
        this.cdr.markForCheck();
      },
    });
  }
}
