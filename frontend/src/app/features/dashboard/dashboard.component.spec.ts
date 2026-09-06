import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideLocationMocks } from '@angular/common/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../core/services/auth.service';
import { DocumentService } from '../../core/services/document.service';
import { StudentService } from '../../core/services/student.service';
import { RoomAllocationService } from '../../core/services/room-allocation.service';
import { ApplicationService } from '../../core/services/application.service';
import { SystemSettingsService } from '../../core/services/system-settings.service';
import { ErrorHandlingService } from '../../core/services/error-handling.service';
import { WaitlistService } from '../../core/services/waitlist.service';
import { Application } from '../../core/models/application.model';

/**
 * GH-151: the dashboard keyed its journey stepper to SUBMITTED and OFFERED, neither of which is a
 * backend status, and omitted ALLOCATED and CHANGES_REQUESTED from its order array, so accepting an
 * offer reset the stepper to blank dots. It also picked the application to display PENDING-first,
 * which contradicted the Room card beside it, and seeded three fabricated document rows.
 */
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let roomAllocationService: jasmine.SpyObj<RoomAllocationService>;
  let documentService: jasmine.SpyObj<DocumentService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const application = (status: string, createdAt: string): Application =>
    ({ id: 1, status, createdAt } as unknown as Application);

  beforeEach(() => {
    const authService = { currentUserValue: { id: 1, email: 'a@b.com' } };
    documentService = jasmine.createSpyObj<DocumentService>('DocumentService', [
      'getUploadedDocuments',
    ]);
    documentService.getUploadedDocuments.and.returnValue(of([]));
    const studentService = jasmine.createSpyObj<StudentService>('StudentService', [
      'getStudentProfile',
    ]);
    roomAllocationService = jasmine.createSpyObj<RoomAllocationService>('RoomAllocationService', [
      'getActiveAllocationByStudentId',
      'acceptOffer',
      'declineOffer',
    ]);
    const applicationService = jasmine.createSpyObj<ApplicationService>('ApplicationService', [
      'getMyApplications',
    ]);
    applicationService.getMyApplications.and.returnValue(of([]));
    const systemSettingsService = jasmine.createSpyObj<SystemSettingsService>(
      'SystemSettingsService',
      ['getPublicSetting']
    );
    const errorService = jasmine.createSpyObj<ErrorHandlingService>('ErrorHandlingService', [
      'showWarning',
      'showErrorSnackbar',
      'parseError',
    ]);
    const waitlistService = jasmine.createSpyObj<WaitlistService>('WaitlistService', [
      'getMyPosition',
    ]);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideLocationMocks(),
        { provide: AuthService, useValue: authService },
        { provide: DocumentService, useValue: documentService },
        { provide: StudentService, useValue: studentService },
        { provide: RoomAllocationService, useValue: roomAllocationService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: SystemSettingsService, useValue: systemSettingsService },
        { provide: ErrorHandlingService, useValue: errorService },
        { provide: WaitlistService, useValue: waitlistService },
      ],
    });
    // The component imports MatDialogModule itself, and a standalone component's own imports take
    // precedence over TestBed providers, so this has to be an override rather than a provider.
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    component = TestBed.createComponent(DashboardComponent).componentInstance;
  });

  // ---------------------------------------------------------------- documents

  it('uploadedDocuments_beforeAnyResponse_isEmpty', () => {
    expect(component.uploadedDocuments).toEqual([]);
  });

  /** The error branch used to leave whatever was on screen, which was three fabricated rows. */
  it('loadDocuments_requestFails_clearsTheList', () => {
    component.uploadedDocuments = [{ name: 'Passport', date: '2026-01-01', status: 'Verified' }];
    documentService.getUploadedDocuments.and.returnValue(
      new (class {
        subscribe(handlers: { error: (e: unknown) => void }) {
          handlers.error(new Error('boom'));
          return { unsubscribe: () => undefined };
        }
      })() as never
    );

    component.loadDocuments();

    expect(component.uploadedDocuments).toEqual([]);
  });

  // ------------------------------------------------------- which application

  /**
   * Students sit at the platform tenant, so this list spans every company they have applied to and
   * a stale application elsewhere used to outrank a live one.
   */
  it('getCurrentApplication_pendingAndAllocated_returnsAllocated', () => {
    const result = component.getCurrentApplication([
      application('PENDING', '2026-09-05T10:00:00'),
      application('ALLOCATED', '2026-09-01T10:00:00'),
    ]);

    expect(result?.status).toBe('ALLOCATED');
  });

  /**
   * The case the backend's own active-status list would get wrong if it were mirrored here: it
   * omits CHECKED_IN so a checked-in student may apply for the next year, which is a write-side
   * rule and says nothing about which application to display.
   */
  it('getCurrentApplication_pendingAndCheckedIn_returnsCheckedIn', () => {
    const result = component.getCurrentApplication([
      application('PENDING', '2026-09-05T10:00:00'),
      application('CHECKED_IN', '2026-08-01T10:00:00'),
    ]);

    expect(result?.status).toBe('CHECKED_IN');
  });

  it('getCurrentApplication_sameStage_returnsTheMostRecent', () => {
    const result = component.getCurrentApplication([
      application('PENDING', '2026-09-01T10:00:00'),
      application('PENDING', '2026-09-05T10:00:00'),
    ]);

    expect(result?.createdAt).toBe('2026-09-05T10:00:00');
  });

  it('getCurrentApplication_onlyClosedApplications_returnsTheMostRecent', () => {
    const result = component.getCurrentApplication([
      application('REJECTED', '2026-09-01T10:00:00'),
      application('CANCELLED', '2026-09-05T10:00:00'),
    ]);

    expect(result?.createdAt).toBe('2026-09-05T10:00:00');
  });

  // ------------------------------------------------------------ journey track

  it('journeySteps_allocated_marksEarlierStagesDoneAndAllocatedActive', () => {
    component.currentApplication = application('ALLOCATED', '2026-09-01T10:00:00');

    const steps = component.journeySteps;

    expect(steps.map((s) => s.label)).toEqual([
      'Submitted',
      'Reviewing',
      'Approved',
      'Awaiting',
      'Allocated',
      'Checked In',
    ]);
    expect(steps.filter((s) => s.done).length).toBe(4);
    expect(steps.find((s) => s.active)?.label).toBe('Allocated');
  });

  /** Under the old order array this status was absent, so every step went blank. */
  it('journeySteps_changesRequested_isOnTheReviewingStage', () => {
    component.currentApplication = application('CHANGES_REQUESTED', '2026-09-01T10:00:00');

    expect(component.journeySteps.find((s) => s.active)?.label).toBe('Reviewing');
  });

  it('showJourney_rejectedApplication_isFalse', () => {
    component.currentApplication = application('REJECTED', '2026-09-01T10:00:00');

    expect(component.showJourney).toBeFalse();
  });

  it('showJourney_pendingApplication_isTrue', () => {
    component.currentApplication = application('PENDING', '2026-09-01T10:00:00');

    expect(component.showJourney).toBeTrue();
  });

  // --------------------------------------------------------- response message

  it('showResponseTimeMessage_pendingApplication_isTrue', () => {
    component.currentApplication = application('PENDING', '2026-09-01T10:00:00');
    component.responseTimeMessage = 'We usually reply within 5 days';

    expect(component.showResponseTimeMessage).toBeTrue();
  });

  // ------------------------------------------------------- accepted date wire

  /**
   * A contract test, not a shape test. The backend accepts an expected check-in date in
   * (now - 1 hour) .. (now + 7 days), compared as server-local wall clock. The picker builds a Date
   * at local midnight, and toISOString shifted that into the previous day east of Greenwich, so the
   * backend rejected the student's own "today".
   */
  describe('accepted date lands inside the window the backend allows', () => {
    function acceptWith(picked: Date): string {
      dialog.open.and.returnValue({ afterClosed: () => of(picked) } as never);
      roomAllocationService.acceptOffer.and.returnValue(of({ id: 7 } as never) as never);
      component.roomAllocation = { id: 7 } as never;

      component.acceptOffer();

      return roomAllocationService.acceptOffer.calls.mostRecent().args[1] as string;
    }

    const atMidnight = (offsetDays: number) => {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      d.setDate(d.getDate() + offsetDays);
      return d;
    };

    /**
     * Holds whatever the time of day, which is the point. A noon stamp on today would fall behind
     * the lower bound every afternoon.
     */
    it('earliestSelectableDate_isAfterTheLowerBound', () => {
      const sent = acceptWith(atMidnight(0));

      expect(new Date(sent).getTime()).toBeGreaterThan(Date.now() - 3600_000);
    });

    /**
     * And the last selectable day stays inside the upper bound whatever the time of day, which is
     * why the picker offers today+6 rather than today+7.
     */
    it('latestSelectableDate_isBeforeTheUpperBound', () => {
      const sent = acceptWith(atMidnight(6));

      expect(new Date(sent).getTime()).toBeLessThanOrEqual(Date.now() + 7 * 86400_000);
    });

    /**
     * No trailing Z and no offset: the backend field is a LocalDateTime, and toISOString shifted
     * a local-midnight Date into the previous day east of Greenwich, so the backend rejected the
     * student's own today (GH-151).
     */
    it('acceptedDate_isSentAsALocalWallClockString', () => {
      const today = atMidnight(0);
      const pad = (n: number) => String(n).padStart(2, '0');
      const expected = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(
        today.getDate()
      )}T23:59:00`;

      expect(acceptWith(today)).toBe(expected);
    });
  });
});
