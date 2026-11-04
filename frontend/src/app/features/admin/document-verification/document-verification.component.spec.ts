import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { NgControl } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DocumentVerificationComponent } from './document-verification.component';
import { AdminService } from '../../../core/services/admin.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';

describe('DocumentVerificationComponent', () => {
  function render() {
    TestBed.configureTestingModule({
      imports: [DocumentVerificationComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AdminService,
          useValue: jasmine.createSpyObj('AdminService', {
            searchDocuments: of({ content: [], totalElements: 0 }),
          }),
        },
        {
          provide: DocumentTypeService,
          useValue: jasmine.createSpyObj('DocumentTypeService', {
            // The real shape of the backend DocumentTypeResponse.
            searchDocumentTypes: of({
              content: [{ id: 3, name: 'ID Copy', isRequired: true }],
              totalElements: 1,
            }),
          }),
        },
      ],
    });
    const fixture = TestBed.createComponent(DocumentVerificationComponent);
    fixture.componentInstance.filtersVisible = true;
    fixture.detectChanges();
    return fixture;
  }

  it('typeFilter_responseWithName_listsTheName', () => {
    const typeFilter = render()
      .debugElement.queryAll(By.directive(MatSelect))
      .find((select) => select.injector.get(NgControl).name === 'documentTypeId')!;

    const labels = (typeFilter.componentInstance as MatSelect).options.map((o) =>
      o.viewValue.trim()
    );

    expect(labels).toEqual(['All Types', 'ID Copy']);
  });
});
