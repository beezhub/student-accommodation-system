import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { DocumentsComponent } from './documents.component';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentService } from '../../../core/services/document.service';
import { DocumentTypeService } from '../../../core/services/document-type.service';
import { DocumentType } from '../../../core/models/document-type.model';
import { DocumentData } from '../../../core/models/document.model';

/**
 * #443: a company can require a type of its own, under a name the platform does not use. Students
 * are platform tenant 0, so the page only offered platform types and the student could never
 * upload it. It now asks for the student's own list, with the buildings an application form
 * linked here with.
 */
describe('DocumentsComponent: company document types', () => {
  const type = (id: number, name: string): DocumentType =>
    ({ id, name, description: '', isRequired: true } as DocumentType);
  const nationalId = type(1, 'National ID');
  const bursaryLetter = type(50, 'Bursary Letter');

  let documentService: jasmine.SpyObj<DocumentService>;
  let documentTypeService: jasmine.SpyObj<DocumentTypeService>;

  function create(queryParams: Record<string, string> = {}): DocumentsComponent {
    TestBed.configureTestingModule({
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: { currentUserValue: { id: 3 } } },
        { provide: DocumentService, useValue: documentService },
        { provide: DocumentTypeService, useValue: documentTypeService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
        },
      ],
    });
    const component = TestBed.createComponent(DocumentsComponent).componentInstance;
    component.ngOnInit();
    return component;
  }

  beforeEach(() => {
    documentService = jasmine.createSpyObj('DocumentService', [
      'getUploadedDocuments',
      'uploadSingleDocumentWithProgress',
      'validateFile',
    ]);
    documentTypeService = jasmine.createSpyObj('DocumentTypeService', ['getMyDocumentTypes']);
    documentService.getUploadedDocuments.and.returnValue(of([]));
    documentTypeService.getMyDocumentTypes.and.returnValue(of([nationalId, bursaryLetter]));
  });

  it('loadPack_linkedFromAForm_asksForThePickedBuildingsCompanyTypes', () => {
    create({ buildings: '5,6' });

    expect(documentTypeService.getMyDocumentTypes).toHaveBeenCalledWith([5, 6]);
  });

  it('loadPack_openedDirectly_asksWithoutBuildings', () => {
    create();

    expect(documentTypeService.getMyDocumentTypes).toHaveBeenCalledWith([]);
  });

  it('loadPack_companyType_getsARequiredSlot', () => {
    const component = create({ buildings: '5' });

    const slot = component.slots.find((s) => s.typeName === 'Bursary Letter');
    expect(slot).toEqual(
      jasmine.objectContaining({ typeId: 50, isRequired: true, status: 'NOT_UPLOADED' })
    );
    expect(component.requiredMissingCount).toBe(2);
  });

  it('loadPack_documentOfACompanyType_showsInItsSlot', () => {
    documentService.getUploadedDocuments.and.returnValue(
      of([
        {
          id: 900,
          documentName: 'bursary.pdf',
          documentTypeId: 50,
          documentTypeName: 'Bursary Letter',
          documentStatus: 'PENDING',
          uploadDate: '2026-11-15T10:00:00',
        } as DocumentData,
      ])
    );

    const component = create();

    const slot = component.slots.find((s) => s.typeId === 50);
    expect(slot?.status).toBe('PENDING');
    expect(slot?.documentId).toBe(900);
    expect(component.requiredMissingCount).toBe(1);
  });

  it('uploadDocument_companySlot_uploadsAgainstTheCompanyType', () => {
    documentService.validateFile.and.returnValue({ valid: true, errors: [] });
    documentService.uploadSingleDocumentWithProgress.and.returnValue(
      of({ status: 'progress' as const, progress: 40 })
    );
    const component = create({ buildings: '5' });
    const file = new File(['%PDF'], 'bursary.pdf', { type: 'application/pdf' });

    component.openUploadModal(component.slots.find((s) => s.typeId === 50)!);
    component.onFileSelected({ target: { files: [file] } } as unknown as Event);
    component.uploadDocument();

    expect(documentService.uploadSingleDocumentWithProgress).toHaveBeenCalledWith(file, 50);
  });
});
