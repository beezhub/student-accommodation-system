import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DocumentTypeManagementComponent } from './document-type-management.component';
import {
  DocumentTypeResponse,
  DocumentTypeService,
} from '../../../core/services/document-type.service';

/** The real shape of the backend DocumentTypeResponse. */
const DOCUMENT_TYPE = {
  id: 3,
  name: 'ID Copy',
  description: 'A copy of your ID',
  isRequired: true,
  isPlatformDefault: false,
} as unknown as DocumentTypeResponse;

describe('DocumentTypeManagementComponent', () => {
  function render() {
    TestBed.configureTestingModule({
      imports: [DocumentTypeManagementComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: DocumentTypeService,
          useValue: jasmine.createSpyObj('DocumentTypeService', {
            searchDocumentTypes: of({ content: [DOCUMENT_TYPE], totalElements: 1 }),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(DocumentTypeManagementComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('list_responseWithName_showsTheName', () => {
    expect(render().querySelector('.type-name')?.textContent?.trim()).toBe('ID Copy');
  });

  it('list_apiSendsNoCreatedDate_hasNoCreatedColumn', () => {
    const headers = Array.from(render().querySelectorAll('th')).map((th) => th.textContent?.trim());

    expect(headers).not.toContain('Created');
  });
});
