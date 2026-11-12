import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ContactInfoService } from './contact-info.service';
import { ContactInfo, telHref } from '../models/contact-info.model';
import { environment } from '../../../environments/environment';

describe('ContactInfoService', () => {
  let service: ContactInfoService;
  let httpMock: HttpTestingController;
  const url = `${environment.apiUrl}/contact`;
  const info: ContactInfo = { email: 'help@example.co.za', phone: '+27 11 555 0100' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ContactInfoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the details once and shares them', () => {
    const results: (ContactInfo | null)[] = [];
    service.getContactInfo().subscribe((r) => results.push(r));
    service.getContactInfo().subscribe((r) => results.push(r));

    const req = httpMock.expectOne(url);
    expect(req.request.headers.get('X-Silent-Error')).toBe('true');
    req.flush(info);

    expect(results).toEqual([info, info]);
  });

  it('emits null on failure and tries again next time', () => {
    let result: ContactInfo | null | undefined;
    service.getContactInfo().subscribe((r) => (result = r));
    httpMock.expectOne(url).flush('down', { status: 503, statusText: 'Service Unavailable' });
    expect(result).toBeNull();

    service.getContactInfo().subscribe((r) => (result = r));
    httpMock.expectOne(url).flush(info);
    expect(result).toEqual(info);
  });
});

describe('telHref', () => {
  it('keeps the plus and the digits only', () => {
    expect(telHref('+27 11 555-0100')).toBe('tel:+27115550100');
  });

  it('drops the (0) after a country code', () => {
    expect(telHref('+27 (0) 11 555 0100')).toBe('tel:+27115550100');
  });

  it('leaves a local number as it is', () => {
    expect(telHref('011 555 0100')).toBe('tel:0115550100');
  });
});
