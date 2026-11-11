import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { DocumentTypeFormDialogComponent } from './document-type-form-dialog.component';
import { DocumentTypeService } from '../../../../core/services/document-type.service';

describe('DocumentTypeFormDialogComponent', () => {
  it('ngOnInit_editMode_fillsTheFormFromTheApiResponse', () => {
    TestBed.configureTestingModule({
      imports: [DocumentTypeFormDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: DocumentTypeService, useValue: {} },
        { provide: MatDialogRef, useValue: { close: () => undefined } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            mode: 'edit',
            documentType: {
              id: 3,
              name: 'ID Copy',
              description: 'A copy',
              isRequired: true,
              isExpiring: true,
            },
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(DocumentTypeFormDialogComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.documentTypeForm.value).toEqual({
      name: 'ID Copy',
      description: 'A copy',
      isRequired: true,
      isExpiring: true,
    });
  });
});
