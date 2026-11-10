import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { InstitutionPromptComponent } from './institution-prompt.component';
import { InstitutionService } from '../../../../../core/services/institution.service';
import { StudentService } from '../../../../../core/services/student.service';
import { StudentResponse } from '../../../../../core/models/student.model';

describe('InstitutionPromptComponent', () => {
  const uct = { id: 1, name: 'UCT' };
  let studentService: jasmine.SpyObj<StudentService>;
  let component: InstitutionPromptComponent;

  beforeEach(() => {
    studentService = jasmine.createSpyObj('StudentService', ['updateStudentProfile']);
    TestBed.configureTestingModule({
      providers: [
        provideNoopAnimations(),
        { provide: InstitutionService, useValue: { getInstitutions: () => of([uct]) } },
        { provide: StudentService, useValue: studentService },
      ],
    });
    component = TestBed.createComponent(InstitutionPromptComponent).componentInstance;
    component.profile = {
      id: 7,
      firstName: 'Sam',
      lastName: 'Student',
      email: 'sam@example.com',
      yearOfStudyId: 2,
    } as StudentResponse;
    component.ngOnInit();
  });

  it('save_keepsTheRestOfTheProfileAndEmitsTheInstitution', () => {
    studentService.updateStudentProfile.and.returnValue(of({} as StudentResponse));
    const saved = spyOn(component.saved, 'emit');
    component.institution.setValue(1);

    component.save();

    const sent = studentService.updateStudentProfile.calls.mostRecent().args[0];
    expect(sent.institutionId).toBe(1);
    expect(sent.yearOfStudyId).toBe(2);
    expect(sent.email).toBe('sam@example.com');
    expect(saved).toHaveBeenCalledWith(uct);
  });

  it('save_nothingChosen_doesNotSave', () => {
    component.save();

    expect(studentService.updateStudentProfile).not.toHaveBeenCalled();
    expect(component.institution.touched).toBeTrue();
  });

  it('save_fails_saysSo', () => {
    studentService.updateStudentProfile.and.returnValue(throwError(() => new Error('x')));
    component.institution.setValue(1);

    component.save();

    expect(component.error).toContain('not saved');
    expect(component.isSaving).toBeFalse();
  });
});
