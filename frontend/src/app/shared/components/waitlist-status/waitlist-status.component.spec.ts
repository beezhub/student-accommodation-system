import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { WaitlistStatusComponent } from './waitlist-status.component';
import { WaitlistPosition } from '../../../core/services/waitlist.service';

describe('WaitlistStatusComponent', () => {
  let fixture: ComponentFixture<WaitlistStatusComponent>;
  let component: WaitlistStatusComponent;

  async function createComponent(position: WaitlistPosition) {
    await TestBed.configureTestingModule({
      imports: [WaitlistStatusComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WaitlistStatusComponent);
    component = fixture.componentInstance;
    component.position = position;
    fixture.detectChanges();
  }

  it('should create', async () => {
    await createComponent({ position: 3, totalInQueue: 12, estimatedWaitDays: 6 });
    expect(component).toBeTruthy();
  });

  it('renders position and queue depth', async () => {
    await createComponent({ position: 3, totalInQueue: 12, estimatedWaitDays: 6 });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain("You're #3 in the queue");
    expect(text).toContain('Position #3 of 12 students waiting');
  });

  it('renders estimated wait time when available (plural days)', async () => {
    await createComponent({ position: 3, totalInQueue: 12, estimatedWaitDays: 6 });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Estimated wait: approximately 6 days');
  });

  it('renders singular "day" when estimate is exactly 1', async () => {
    await createComponent({ position: 1, totalInQueue: 1, estimatedWaitDays: 1 });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Estimated wait: approximately 1 day');
    expect(text).not.toContain('1 days');
  });

  it('renders unavailable message when estimatedWaitDays is null', async () => {
    await createComponent({ position: 2, totalInQueue: 4, estimatedWaitDays: null });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Estimated wait time unavailable — allocations are processed regularly');
    const unavailableEl = fixture.debugElement.query(
      By.css('.waitlist-card__estimate--unavailable')
    );
    expect(unavailableEl).toBeTruthy();
  });

  it('always renders the notification promise text', async () => {
    await createComponent({ position: 1, totalInQueue: 1, estimatedWaitDays: null });

    const text = fixture.nativeElement.textContent;
    expect(text).toContain(
      "You'll be notified by email as soon as a room becomes available for you"
    );
  });
});
