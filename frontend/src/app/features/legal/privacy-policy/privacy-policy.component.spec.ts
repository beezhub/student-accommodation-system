import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { PrivacyPolicyComponent } from './privacy-policy.component';
import { ContactInfoService } from '../../../core/services/contact-info.service';
import { RoleService } from '../../../core/services/role.service';
import { ContactInfo } from '../../../core/models/contact-info.model';

describe('PrivacyPolicyComponent', () => {
  let contact$: Subject<ContactInfo | null>;
  let roleService: jasmine.SpyObj<RoleService>;

  function render(): HTMLElement {
    const fixture = TestBed.createComponent(PrivacyPolicyComponent);
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
    roleService = jasmine.createSpyObj<RoleService>('RoleService', ['isStudent']);
    roleService.isStudent.and.returnValue(false);
    TestBed.configureTestingModule({
      imports: [PrivacyPolicyComponent],
      providers: [
        provideRouter([]),
        { provide: ContactInfoService, useValue: { getContactInfo: () => contact$ } },
        { provide: RoleService, useValue: roleService },
      ],
    });
  });

  it('render_always_showsThePolicyWithWhatPopiaRequires', () => {
    const el = render();

    expect(el.querySelector('h1')?.textContent).toContain('Privacy Policy');
    const headings = Array.from(el.querySelectorAll('h2')).map((h) => h.textContent?.trim());
    expect(headings).toEqual(
      jasmine.arrayContaining([
        'What we collect',
        'Why we use it',
        'Who receives it',
        'How long we keep it',
        'Your rights',
        'Contact us',
      ])
    );
    expect(el.textContent).toContain('Boitumelo Tshehla');
    expect(el.textContent).toContain('Information Regulator');
  });

  it('contact_loaded_showsTheSupportEmailAndPhoneAsLinks', () => {
    const el = emit({ email: 'help@example.co.za', phone: '+27 11 555 0100' }, render());

    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('a.contact-value'));
    expect(links.map((a) => a.getAttribute('href'))).toEqual([
      'mailto:help@example.co.za',
      'tel:+27115550100',
    ]);
  });

  it('contact_noPhoneConfigured_hidesThePhone', () => {
    const el = emit({ email: 'help@example.co.za', phone: null }, render());

    expect(el.querySelectorAll('a.contact-value').length).toBe(1);
    expect(el.querySelector('#contact')?.parentElement?.textContent).not.toContain('Phone:');
  });

  it('contact_loading_saysSo', () => {
    const el = render();

    expect(el.querySelector('.contact-status')?.textContent).toContain('Loading');
    expect(el.querySelector('a.contact-value')).toBeNull();
  });

  it('contact_failedToLoad_saysSo', () => {
    const el = emit(null, render());

    expect(el.querySelector('.unavailable')).not.toBeNull();
    expect(el.querySelector('a.contact-value')).toBeNull();
  });

  it('backLink_signedInStudent_goesToTheDashboard', () => {
    roleService.isStudent.and.returnValue(true);

    const back = render().querySelector<HTMLAnchorElement>('a.back-link');

    expect(back?.getAttribute('href')).toBe('/dashboard');
    expect(back?.textContent).toContain('Back to dashboard');
  });

  it('backLink_visitor_goesHome', () => {
    const back = render().querySelector<HTMLAnchorElement>('a.back-link');

    expect(back?.getAttribute('href')).toBe('/home');
    expect(back?.textContent).toContain('Back to home');
  });

  it('regulatorLink_always_opensInANewTab', () => {
    const link = render().querySelector<HTMLAnchorElement>('a.regulator-link');

    expect(link?.getAttribute('href')).toBe('https://inforegulator.org.za');
    expect(link?.target).toBe('_blank');
    expect(link?.rel).toContain('noopener');
  });
});
