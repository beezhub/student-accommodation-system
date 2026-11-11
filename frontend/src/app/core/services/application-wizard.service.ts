import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { Application } from '../models/application.model';
import { StudentRequest } from '../models/student.model';
import { User } from '../models/user.model';
import { toLocalDateString } from '../utils/local-date';
import { ApplicationService } from './application.service';
import { StudentService } from './student.service';

/** What step 4 of the application wizard collects. */
export interface WizardStep4 {
  studentId: number;
  institutionId: number;
  yearOfStudyId: number;
  institutionName: string;
  studentNumber: string;
  phoneNumber: string;
  dateOfBirth: Date | string;
  gender: string;
  courseName: string;
  requiresAccessibility: boolean;
  preferredBuildingIds: number[];
  specialRequirements: string | null;
}

/**
 * Saves step 4 of the application wizard: the profile fields first, then the application, so a
 * student who refreshes or changes device after Continue to Review has lost nothing (#538).
 */
@Injectable({ providedIn: 'root' })
export class ApplicationWizardService {
  private readonly studentService = inject(StudentService);
  private readonly applicationService = inject(ApplicationService);

  /** Returns the applications created, one per company (GH-495). */
  submit(user: User, step: WizardStep4): Observable<Application[]> {
    const profile: StudentRequest = {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      studentNumber: step.studentNumber,
      phoneNumber: step.phoneNumber,
      // A calendar day: toISOString would move a local midnight to the day before (#464).
      dateOfBirth: toLocalDateString(step.dateOfBirth),
      gender: step.gender,
      specialRequirements: step.specialRequirements ?? undefined,
      institutionId: step.institutionId,
      yearOfStudyId: step.yearOfStudyId,
    };

    return this.studentService.updateStudentProfile(profile).pipe(
      switchMap(() =>
        this.applicationService.createApplication({
          studentId: step.studentId,
          requiresAccessibility: step.requiresAccessibility,
          preferredBuildingIds: step.preferredBuildingIds.length
            ? step.preferredBuildingIds
            : undefined,
          specialRequirements: step.specialRequirements || null,
          institutionName: step.institutionName,
          courseName: step.courseName,
        })
      )
    );
  }
}
