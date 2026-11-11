import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApplyOptionsService } from '../../../core/services/apply-options.service';
import { CompanyApplyOptions } from '../../../core/models/apply-options.model';
import { describeIntake } from '../../../core/utils/apply-wording';

/**
 * Dashboard banner for application windows open now (GH-495): each company that can take the
 * student, with an intake they have not applied for yet. Shows nothing when none is open, or when
 * the lookup fails, since the rest of the dashboard does not depend on it.
 */
@Component({
  selector: 'app-open-windows-banner',
  templateUrl: './open-windows-banner.component.html',
  styleUrls: ['./open-windows-banner.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, MatButtonModule, MatIconModule],
})
export class OpenWindowsBannerComponent implements OnInit {
  private readonly applyOptions = inject(ApplyOptionsService);
  private readonly cdr = inject(ChangeDetectorRef);

  windows: CompanyApplyOptions[] = [];

  ngOnInit(): void {
    this.applyOptions.getOpenWindows().subscribe({
      next: (windows) => {
        this.windows = windows;
        this.cdr.markForCheck();
      },
      error: () => {
        this.windows = [];
        this.cdr.markForCheck();
      },
    });
  }

  /** "Company A: Semester 2, 2027. Move in 15 July. Apply by 30 June." */
  describe(window: CompanyApplyOptions): string {
    return `${window.companyName}: ${describeIntake(window.openIntakes[0])}`;
  }
}
