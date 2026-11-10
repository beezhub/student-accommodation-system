import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Observable } from 'rxjs';

import { AcademicYearService } from './academic-year.service';
import { AccommodationTypeService } from './accommodation-type.service';
import { DocumentTypeService } from './document-type.service';
import { InstitutionService } from './institution.service';
import { YearOfStudyService } from './year-of-study.service';

/**
 * #310 (part of #303): every failure on the reference-data admin pages showed two snackbars, the
 * errorInterceptor's generic one and the page's own. One message per failure:
 *
 * - writes are always silent: each has one caller, a page or form dialog that says what failed;
 * - reads are silent only when the caller asks, because document verification lists document
 *   types with no message of its own and relies on the interceptor's.
 */
describe('Reference-data services: one message per failure', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Subscribes, then reports whether the one request it made asked the interceptor to stay quiet. */
  function silenced(call: () => Observable<unknown>): boolean {
    call().subscribe();
    const req = http.expectOne(() => true);
    const silent = req.request.headers.get('X-Silent-Error') === 'true';
    req.flush({ content: [], totalElements: 0 });
    return silent;
  }

  const academicYears = () => TestBed.inject(AcademicYearService);
  const accommodationTypes = () => TestBed.inject(AccommodationTypeService);
  const documentTypes = () => TestBed.inject(DocumentTypeService);
  const institutions = () => TestBed.inject(InstitutionService);
  const yearsOfStudy = () => TestBed.inject(YearOfStudyService);

  const writes: [string, () => Observable<unknown>][] = [
    ['academic year create', () => academicYears().createAcademicYear({} as never)],
    ['academic year update', () => academicYears().updateAcademicYear(1, {} as never)],
    ['academic year delete', () => academicYears().deleteAcademicYear(1)],
    ['academic year from pattern', () => academicYears().createFromPattern({} as never)],
    ['academic year copy', () => academicYears().copyAcademicYear(1)],
    ['intake create', () => academicYears().createIntake(1, {} as never)],
    ['intake update', () => academicYears().updateIntake(1, {} as never)],
    ['intake delete', () => academicYears().deleteIntake(1)],
    ['intake close applications', () => academicYears().closeApplicationsNow(1)],
    ['accommodation type create', () => accommodationTypes().create({} as never)],
    ['accommodation type update', () => accommodationTypes().update(1, {} as never)],
    ['accommodation type delete', () => accommodationTypes().delete(1)],
    ['document type create', () => documentTypes().createDocumentType({} as never)],
    ['document type update', () => documentTypes().updateDocumentType(1, {} as never)],
    ['document type required flag', () => documentTypes().updateRequiredFlag(1, true)],
    ['document type delete', () => documentTypes().deleteDocumentType(1)],
    ['institution create', () => institutions().createInstitution({} as never)],
    ['institution update', () => institutions().updateInstitution(1, {} as never)],
    ['institution delete', () => institutions().deleteInstitution(1)],
    ['year of study create', () => yearsOfStudy().createYearOfStudy({} as never)],
    ['year of study update', () => yearsOfStudy().updateYearOfStudy(1, {} as never)],
    ['year of study delete', () => yearsOfStudy().deleteYearOfStudy(1)],
  ];

  for (const [name, call] of writes) {
    it(`${name}_isSilent_soOnlyTheFormsMessageShows`, () => {
      expect(silenced(call)).toBeTrue();
    });
  }

  const reads: [string, (silent?: boolean) => Observable<unknown>][] = [
    ['academic years list', (silent) => academicYears().getAcademicYears(silent)],
    ['accommodation types list', (silent) => accommodationTypes().getAll(0, 20, 'name', silent)],
    ['document types search', (silent) => documentTypes().searchDocumentTypes({}, silent)],
    ['institutions search', (silent) => institutions().searchInstitutions({}, silent)],
    ['years of study search', (silent) => yearsOfStudy().searchYearsOfStudy({}, silent)],
  ];

  for (const [name, call] of reads) {
    it(`${name}_isSilentWhenTheCallerAsks`, () => {
      expect(silenced(() => call(true))).toBeTrue();
    });

    it(`${name}_keepsTheInterceptorsMessageByDefault`, () => {
      expect(silenced(() => call())).toBeFalse();
    });
  }
});
