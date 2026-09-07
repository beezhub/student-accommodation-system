import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import {
  DeclineOfferDialogComponent,
  DeclineOfferDialogData,
} from './decline-offer-dialog.component';
import { DeclineReason } from '../../../core/services/room-allocation.service';

/**
 * GH-159: the decline half of the student offer path had no spec.
 *
 * <p>Declining is irreversible from the student's side — the room goes straight back through the
 * waiting-list refill — so what this dialog sends, and what it refuses to send, is worth pinning
 * down.
 */
describe('DeclineOfferDialogComponent', () => {
  let fixture: ComponentFixture<DeclineOfferDialogComponent>;
  let component: DeclineOfferDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DeclineOfferDialogComponent>>;

  function build(data: Partial<DeclineOfferDialogData> = {}): void {
    TestBed.resetTestingModule();
    dialogRef = jasmine.createSpyObj<MatDialogRef<DeclineOfferDialogComponent>>('MatDialogRef', [
      'close',
    ]);

    TestBed.configureTestingModule({
      imports: [DeclineOfferDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            allocationId: 1,
            roomNumber: 'A101',
            buildingName: 'Test Hall',
            previousDeclineCount: 0,
            ...data,
          } as DeclineOfferDialogData,
        },
      ],
    });

    fixture = TestBed.createComponent(DeclineOfferDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('submit_withNoReasonChosen_sendsNothing', () => {
    build();

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('submit_withAReason_closesWithTheDeclineRequest', () => {
    build();
    component.form.patchValue({ declineReason: DeclineReason.TOO_EXPENSIVE });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      declineReason: DeclineReason.TOO_EXPENSIVE,
      declineReasonText: undefined,
    });
  });

  /** Empty free text is omitted rather than sent as '', so the backend stores null, not blank. */
  it('submit_withEmptyFreeText_omitsItRatherThanSendingAnEmptyString', () => {
    build();
    component.form.patchValue({
      declineReason: DeclineReason.PERSONAL_REASONS,
      declineReasonText: '',
    });

    component.submit();

    const request = dialogRef.close.calls.mostRecent().args[0];
    expect(request.declineReasonText).toBeUndefined();
  });

  it('submit_withFreeText_includesIt', () => {
    build();
    component.form.patchValue({
      declineReason: DeclineReason.OTHER,
      declineReasonText: 'Moving in with family',
    });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      declineReason: DeclineReason.OTHER,
      declineReasonText: 'Moving in with family',
    });
  });

  it('freeText_longerThan500Characters_isRejectedAndNotSent', () => {
    build();
    component.form.patchValue({
      declineReason: DeclineReason.OTHER,
      declineReasonText: 'x'.repeat(501),
    });

    expect(component.form.valid).toBeFalse();

    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  // ── The declines-remaining counter ──────────────────────────────────────────────────────

  it('declinesRemaining_countsDownFromTwo', () => {
    build({ previousDeclineCount: 0 });
    expect(component.declinesRemaining).toBe(2);

    build({ previousDeclineCount: 1 });
    expect(component.declinesRemaining).toBe(1);

    build({ previousDeclineCount: 2 });
    expect(component.declinesRemaining).toBe(0);
  });

  /**
   * On the last decline the dialog warns that the application will be closed.
   *
   * <p>Asserted as the frontend's stated behaviour, not as a system guarantee. Nothing in
   * {@code WaitingListService.reQueueStudent} enforces a decline limit — it re-queues on every
   * decline whatever the count — so this warning currently promises something the backend does
   * not do. Left as-is and raised separately, because whether a limit should exist is a product
   * decision, not a test fix.
   */
  it('finalDecline_warnsThatTheApplicationWillBeClosed', () => {
    build({ previousDeclineCount: 2 });

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(component.declinesRemaining).toBe(0);
    expect(text).toContain('Final Decline Warning');
  });

  it('beforeTheLastDecline_doesNotShowTheFinalWarning', () => {
    build({ previousDeclineCount: 0 });

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Final Decline Warning');
  });
});
