import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { DocumentExpiryNoticeComponent } from './document-expiry-notice.component';
import { DocumentService } from '../../../../core/services/document.service';
import { DocumentExpiry } from '../../../../core/models/document.model';

describe('DocumentExpiryNoticeComponent', () => {
  let fixture: ComponentFixture<DocumentExpiryNoticeComponent>;
  let component: DocumentExpiryNoticeComponent;
  let service: jasmine.SpyObj<DocumentService>;

  const proof: DocumentExpiry = {
    documentId: 50,
    documentTypeId: 2,
    documentTypeName: 'Proof of Registration',
    expiryDate: '2026-11-30',
    expired: false,
    dueForReplacement: true,
  };

  function build(result: ReturnType<DocumentService['getMyDocumentExpiries']>): void {
    service = jasmine.createSpyObj<DocumentService>('DocumentService', ['getMyDocumentExpiries']);
    service.getMyDocumentExpiries.and.returnValue(result);
    TestBed.configureTestingModule({
      imports: [DocumentExpiryNoticeComponent],
      providers: [{ provide: DocumentService, useValue: service }],
    });
    fixture = TestBed.createComponent(DocumentExpiryNoticeComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('pack', []);
    fixture.detectChanges();
  }

  function text(): string {
    return (fixture.nativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('due soon: one date and one action', () => {
    build(of([proof]));

    expect(text()).toContain(
      'Your proof of registration expires on 30 November. Upload the new one.'
    );
  });

  it('expired: says it expired', () => {
    build(of([{ ...proof, expired: true }]));

    expect(text()).toContain('Your proof of registration expired on 30 November.');
  });

  it('not yet due: shows nothing', () => {
    build(of([{ ...proof, dueForReplacement: false }]));

    expect(text()).toBe('');
  });

  it('upload: emits the document to replace', () => {
    build(of([proof]));
    const emitted: DocumentExpiry[] = [];
    component.upload.subscribe((e) => emitted.push(e));

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();

    expect(emitted).toEqual([proof]);
  });

  it('a new pack reloads; a failure shows nothing', () => {
    build(throwError(() => new Error('down')));
    expect(text()).toBe('');

    service.getMyDocumentExpiries.and.returnValue(of([proof]));
    fixture.componentRef.setInput('pack', [{}]);
    fixture.detectChanges();

    expect(service.getMyDocumentExpiries).toHaveBeenCalledTimes(2);
    expect(text()).toContain('expires on 30 November');
  });
});
