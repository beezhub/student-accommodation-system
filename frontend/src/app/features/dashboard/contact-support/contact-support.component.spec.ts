import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { ContactSupportComponent } from './contact-support.component';
import { ContactInfoService } from '../../../core/services/contact-info.service';
import { ContactInfo } from '../../../core/models/contact-info.model';

describe('ContactSupportComponent', () => {
  let contact$: Subject<ContactInfo | null>;

  function render(): HTMLElement {
    const fixture = TestBed.createComponent(ContactSupportComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function emit(value: ContactInfo | null, el: HTMLElement): HTMLElement {
    contact$.next(value);
    TestBed.tick();
    return el;
  }

  beforeEach(() => {
    contact$ = new Subject<ContactInfo | null>();
    TestBed.configureTestingModule({
      imports: [ContactSupportComponent],
      providers: [{ provide: ContactInfoService, useValue: { getContactInfo: () => contact$ } }],
    });
  });

  it('shows a spinner while loading', () => {
    const el = render();
    expect(el.querySelector('mat-spinner')).not.toBeNull();
  });

  it('shows the email and phone as links', () => {
    const el = emit({ email: 'help@example.co.za', phone: '+27 11 555 0100' }, render());

    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('a.contact-value'));
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      'mailto:help@example.co.za',
      'tel:+27115550100',
    ]);
    expect(el.querySelector('form')).toBeNull();
  });

  it('hides the phone row when none is configured', () => {
    const el = emit({ email: 'help@example.co.za', phone: null }, render());

    expect(el.querySelectorAll('a.contact-value').length).toBe(1);
    expect(el.textContent).not.toContain('Phone');
  });

  it('says so when the details cannot be loaded', () => {
    const el = emit(null, render());

    expect(el.querySelector('.unavailable')).not.toBeNull();
  });
});
