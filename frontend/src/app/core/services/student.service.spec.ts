import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { StudentService } from './student.service';
import { StudentRequest } from '../models/student.model';
import { environment } from '../../../environments/environment';

describe('StudentService', () => {
  const apiUrl = `${environment.apiUrl}/student`;
  const request = { firstName: 'Ann' } as StudentRequest;

  let authService: { currentUserValue: { id: string } | null };
  let service: StudentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    authService = { currentUserValue: { id: '7' } };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    });
    service = TestBed.inject(StudentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('createStudentProfile_postsTheRequest', () => {
    service.createStudentProfile(request).subscribe();

    const call = httpMock.expectOne(apiUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush({});
  });

  it('getStudentProfile_asksForTheCurrentStudent', () => {
    service.getStudentProfile().subscribe();

    expect(httpMock.expectOne(`${apiUrl}/me`).request.method).toBe('GET');
  });

  it('updateStudentProfile_signedIn_putsToTheirOwnId', () => {
    service.updateStudentProfile(request).subscribe();

    expect(httpMock.expectOne(`${apiUrl}/7`).request.method).toBe('PUT');
  });

  it('updateStudentProfile_signedOut_throws', () => {
    authService.currentUserValue = null;

    expect(() => service.updateStudentProfile(request)).toThrowError(/not logged in/);
  });

  it('getAllStudents_sendsTheFiltersAsParams', () => {
    service.getAllStudents({ page: 2 }).subscribe();

    const call = httpMock.expectOne((r) => r.url === apiUrl);
    expect(call.request.params.get('page')).toBe('2');
  });

  it('searchStudents_sendsTheFiltersAsParams', () => {
    service.searchStudents({ page: 3 }).subscribe();

    const call = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
    expect(call.request.params.get('page')).toBe('3');
  });

  it('deleteStudent_deletesById', () => {
    service.deleteStudent(9).subscribe();

    expect(httpMock.expectOne(`${apiUrl}/9`).request.method).toBe('DELETE');
  });
});
