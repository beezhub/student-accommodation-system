import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AcceptOfferDialogComponent } from './accept-offer-dialog.component';

/**
 * GH-151: the picker derived its window from when the offer was made, while the backend validates
 * the expected check-in date against acceptance time. A student accepting late got a needlessly
 * narrow picker, and if the offer window were ever configured longer than the check-in window,
 * maxDate fell before minDate and no date was selectable at all.
 */
describe('AcceptOfferDialogComponent date window', () => {
  const NOW = new Date(2026, 8, 6, 15, 0, 0);

  function build(allocation: Record<string, unknown>): AcceptOfferDialogComponent {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [AcceptOfferDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: { allocation } },
      ],
    });
    return TestBed.createComponent(AcceptOfferDialogComponent).componentInstance;
  }

  const midnight = (d: Date) => {
    const copy = new Date(d);
    copy.setHours(0, 0, 0, 0);
    return copy;
  };

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(NOW);
  });

  afterEach(() => jasmine.clock().uninstall());

  it('constructor_offerMadeSixDaysAgo_minDateIsTodayNotTheOfferDate', () => {
    const component = build({ id: 1, createdAt: new Date(2026, 7, 31).toISOString() });

    expect(component.minDate).toEqual(midnight(NOW));
  });

  /**
   * One day short of the seven-day window. The emitted time is 23:59 and the backend compares
   * against now plus seven days at the current time of day, so today+7 would be rejected for any
   * acceptance before 23:59.
   */
  it('constructor_anyOffer_maxDateIsSixDaysAfterToday', () => {
    const component = build({ id: 1, createdAt: new Date(2026, 8, 1).toISOString() });

    const expected = midnight(NOW);
    expected.setDate(expected.getDate() + 6);
    expect(component.maxDate).toEqual(expected);
  });

  /** The window no longer shrinks as the offer ages, so there is always a selectable day. */
  it('constructor_offerMadeLongAgo_stillLeavesSelectableDays', () => {
    const component = build({ id: 1, createdAt: new Date(2026, 7, 1).toISOString() });

    expect(component.maxDate.getTime()).toBeGreaterThan(component.minDate.getTime());
  });

  /** GH-496: an offer made months before move-in counts its window from move-in day. */
  it('constructor_stayStartsLater_windowStartsOnMoveInDay', () => {
    const component = build({ id: 1, stayStartDate: '2027-02-01', stayEndDate: '2027-11-30' });

    expect(component.moveInDay).toEqual(new Date(2027, 1, 1));
    expect(component.minDate).toEqual(new Date(2027, 1, 1));
    expect(component.maxDate).toEqual(new Date(2027, 1, 7));
  });

  it('constructor_stayAlreadyStarted_windowStartsToday', () => {
    const component = build({ id: 1, stayStartDate: '2026-02-01', stayEndDate: '2026-11-30' });

    expect(component.moveInDay).toBeNull();
    expect(component.minDate).toEqual(midnight(NOW));
  });
});
