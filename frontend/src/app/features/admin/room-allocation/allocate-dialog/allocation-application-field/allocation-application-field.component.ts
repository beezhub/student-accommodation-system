import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject,
  input,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminService, ApplicationResponse } from '../../../../../core/services/admin.service';

/**
 * The optional application picker of the allocate dialog, searching approved applications by
 * code. Binds to the dialog's form, which must have an `applicationId` control.
 */
@Component({
  selector: 'app-allocation-application-field',
  templateUrl: './allocation-application-field.component.html',
  styleUrl: './allocation-application-field.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
  ],
  // The field sits in the dialog's form column as if it were its own.
  host: { style: 'display: contents' },
})
export class AllocationApplicationFieldComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly adminService = inject(AdminService);

  readonly group = input.required<FormGroup>();

  /** Options fetched per search. Small on purpose: these feed a dropdown, not a report. */
  readonly optionPageSize = 25;
  readonly applicationSearch = new FormControl('');
  studentApplications: ApplicationResponse[] = [];
  moreApplicationsExist = false;

  ngOnInit(): void {
    this.loadApplications();

    this.applicationSearch.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((term) => this.loadApplications(term ?? ''));
  }

  /** One page of approved applications, narrowed by the code the admin types (GH-157). */
  loadApplications(applicationCode?: string): void {
    this.adminService
      .searchApplications({
        status: 'APPROVED',
        page: 0,
        size: this.optionPageSize,
        applicationCode: applicationCode || undefined,
      })
      .subscribe({
        next: (response) => {
          this.studentApplications = response.content;
          this.moreApplicationsExist = response.totalElements > response.content.length;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Failed to load applications:', error);
        },
      });
  }
}
