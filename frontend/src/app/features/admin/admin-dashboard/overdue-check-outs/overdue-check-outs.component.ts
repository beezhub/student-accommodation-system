import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AdminService } from '../../../../core/services/admin.service';
import { RoomAllocationResponse } from '../../../../core/services/room-allocation.service';

/**
 * Overdue check-outs on the admin dashboard (GH-496): students still checked in after their stay
 * ended. Nothing checks them out automatically, and the next student cannot check into that bed
 * until they do. Shows nothing when there are none, or when the lookup fails.
 */
@Component({
  selector: 'app-overdue-check-outs',
  templateUrl: './overdue-check-outs.component.html',
  styleUrls: ['./overdue-check-outs.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterModule, MatIconModule],
})
export class OverdueCheckOutsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly cdr = inject(ChangeDetectorRef);

  overdue: RoomAllocationResponse[] = [];
  total = 0;

  ngOnInit(): void {
    this.adminService.getOverdueCheckOuts().subscribe({
      next: (page) => {
        this.overdue = page.content;
        this.total = page.totalElements;
        this.cdr.markForCheck();
      },
      error: () => {
        this.overdue = [];
        this.total = 0;
        this.cdr.markForCheck();
      },
    });
  }
}
