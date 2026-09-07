import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { ProfileSetupComponent } from './profile-setup.component';
import { AuthService } from '../../../core/services/auth.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { StudentService } from '../../../core/services/student.service';
import { ErrorHandlingService } from '../../../core/services/error-handling.service';
import { User } from '../../../core/models/user.model';
import { StudentResponse } from '../../../core/models/student.model';

/**
 * GH-239: the emergency contact is collected here, where the profile is first built.
 *
 * <p>It is required on this form rather than only at the point of application, so that a student
 * is not stopped later by a form they have already left behind — the backend refuses an
 * application without a name and a phone.
 */
describe('ProfileSetupComponent — emergency contact', () => {
  let fixture: ComponentFixture<ProfileSetupComponent>;
  let component: ProfileSetupComponent;
  let studentService: jasmine.SpyObj<StudentService>;

  const user: User = {
    id: '7',
    firstName: 'Sam',
    lastName: 'Student',
    email: 'sam@example.com',
    profileCompleted: false,
    userRole: 'Student',
  };

  beforeEach(async () => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', [], {
      currentUserValue: user,
    });
    const institutionService = jasmine.createSpyObj<InstitutionService>('InstitutionService', [
      'getInstitutions',
    ]);
    institutionService.getInstitutions.and.returnValue(of([]));
    const yearOfStudyService = jasmine.createSpyObj<YearOfStudyService>('YearOfStudyService', [
      'getYearsOfStudy',
    ]);
    yearOfStudyService.getYearsOfStudy.and.returnValue(of([]));
    studentService = jasmine.createSpyObj<StudentService>('StudentService', [
      'createStudentProfile',
    ]);
    studentService.createStudentProfile.and.returnValue(of({} as StudentResponse));
    const errorService = jasmine.createSpyObj<ErrorHandlingService>('ErrorHandlingService', [
      'showErrorSnackbar',
      'showWarning',
      'parseError',
    ]);

    await TestBed.configureTestingModule({
      imports: [ProfileSetupComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: InstitutionService, useValue: institutionService },
        { provide: YearOfStudyService, useValue: yearOfStudyService },
        { provide: StudentService, useValue: studentService },
        { provide: ErrorHandlingService, useValue: errorService },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileSetupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function fillRequiredExceptContact(): void {
    component.profileForm.patchValue({ institution: 1, yearOfStudy: 1 });
  }

  it('theFormIsIncompleteWithoutAnEmergencyContact', () => {
    fillRequiredExceptContact();

    expect(component.profileForm.valid).toBeFalse();
    expect(component.profileForm.get('emergencyContactName')?.hasError('required')).toBeTrue();
    expect(component.profileForm.get('emergencyContactPhone')?.hasError('required')).toBeTrue();
  });

  it('aNameAndPhoneAreEnough_relationshipAndEmailAreOptional', () => {
    fillRequiredExceptContact();
    component.profileForm.patchValue({
      emergencyContactName: 'Thandi Mokoena',
      emergencyContactPhone: '+27 82 123 4567',
    });

    expect(component.profileForm.valid).toBeTrue();
  });

  /**
   * The student's own number keeps the 10-digit domestic rule; this field must not, because a
   * student's family may not be in the country — exactly when the record matters most.
   */
  it('anInternationalNumberIsAccepted', () => {
    const phone = component.profileForm.get('emergencyContactPhone');

    phone?.setValue('+44 20 7946 0958');
    expect(phone?.valid).toBeTrue();

    phone?.setValue('+1 (555) 123-4567');
    expect(phone?.valid).toBeTrue();
  });

  it('somethingThatIsNotAPhoneNumberIsRejected', () => {
    const phone = component.profileForm.get('emergencyContactPhone');

    phone?.setValue('call my mum');

    expect(phone?.hasError('pattern')).toBeTrue();
  });

  it('submit_sendsTheContactWithTheProfile', () => {
    fillRequiredExceptContact();
    component.profileForm.patchValue({
      emergencyContactName: 'Thandi Mokoena',
      emergencyContactRelationship: 'Mother',
      emergencyContactPhone: '+27 82 123 4567',
      emergencyContactEmail: 'thandi@example.com',
    });

    component.onSubmit();

    const sent = studentService.createStudentProfile.calls.mostRecent().args[0];
    expect(sent.emergencyContactName).toBe('Thandi Mokoena');
    expect(sent.emergencyContactRelationship).toBe('Mother');
    expect(sent.emergencyContactPhone).toBe('+27 82 123 4567');
    expect(sent.emergencyContactEmail).toBe('thandi@example.com');
  });
});
