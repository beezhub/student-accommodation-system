import {
  Component,
  OnInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';
import { StudentService } from '../../../core/services/student.service';
import { StudentResponse } from '../../../core/models/student.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { Institution } from '../../../core/models/institution.model';
import { ApplicationService } from '../../../core/services/application.service';
import { Application } from '../../../core/models/application.model';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { contentOf } from '../../../core/models/page-response.model';

/**
 * Step 4's summary. The application is already saved when the student arrives here (#538), so this
 * page only shows what the server holds: the profile and the applications just created, whose ids
 * come in the `ids` query parameter.
 */
@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule,
  ],
})
export class ApplicationReviewComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly studentService = inject(StudentService);
  private readonly yearOfStudyService = inject(YearOfStudyService);
  private readonly institutionService = inject(InstitutionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly errorService = inject(ErrorHandlingService);
  private readonly cdr = inject(ChangeDetectorRef);

  isLoading = true;
  currentUser: User | null = null;
  studentProfile: StudentResponse | null = null;
  yearOfStudy: readonly YearOfStudy[] = [];
  institutions: readonly Institution[] = [];
  applications: Application[] = [];

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    const ids = (this.route.snapshot.queryParamMap.get('ids') ?? '')
      .split(',')
      .map(Number)
      .filter((id) => id > 0);
    if (ids.length === 0) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.studentService.getStudentProfile().subscribe({
      next: (student) => {
        this.studentProfile = student;
        this.cdr.markForCheck();
      },
      error: () => this.errorService.showWarning('Could not load your profile.'),
    });

    this.applicationService.getMyApplications().subscribe({
      next: (applications) => {
        this.applications = applications.filter((a) => ids.includes(a.id));
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.errorService.showWarning(
          'Could not load your application. Find it in your dashboard.'
        );
        this.cdr.markForCheck();
      },
    });

    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (yearOfStudy) => {
        this.yearOfStudy = contentOf(yearOfStudy);
        this.cdr.markForCheck();
      },
      error: () => this.errorService.showWarning('Could not load year of study data.'),
    });

    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = contentOf(institutions);
        this.cdr.markForCheck();
      },
      error: () => this.errorService.showWarning('Could not load institution data.'),
    });
  }

  finish(): void {
    const first = this.applications[0];
    if (first) {
      this.router.navigate(['/application-confirmation', first.id]);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  buildingNames(application: Application): string {
    const buildings = [...(application.preferredBuildings ?? [])].sort(
      (a, b) => a.preferenceOrder - b.preferenceOrder
    );
    if (buildings.length > 0) return buildings.map((b) => b.buildingName).join(', ');
    return application.preferredBuildingName || 'No preference';
  }

  getInstitutionName(): string {
    if (!this.institutions || !this.studentProfile?.institutionId) {
      return 'N/A';
    }
    const institutionId = String(this.studentProfile.institutionId);
    const inst = this.institutions.find((i) => String(i.id) === institutionId);
    return inst?.name || 'N/A';
  }

  getYearOfStudyName(): string {
    if (!this.yearOfStudy || !this.studentProfile?.yearOfStudyId) {
      return 'N/A';
    }
    const yearOfStudyId = String(this.studentProfile.yearOfStudyId);
    const year = this.yearOfStudy.find((y) => String(y.id) === yearOfStudyId);
    return year?.yearOfStudy || 'N/A';
  }
}
