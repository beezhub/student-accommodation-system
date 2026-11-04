import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { PreferencesDialogComponent } from './preferences-dialog.component';

describe('PreferencesDialogComponent', () => {
  it('header_responseWithStudentName_showsTheName', () => {
    TestBed.configureTestingModule({
      imports: [PreferencesDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: () => undefined } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { application: { applicationCode: 'APP-1', studentName: 'Thandi Mokoena' } },
        },
      ],
    });
    const fixture = TestBed.createComponent(PreferencesDialogComponent);
    fixture.detectChanges();

    const name = (fixture.nativeElement as HTMLElement).querySelector('.student-subtitle .name');
    expect(name?.textContent?.trim()).toBe('Thandi Mokoena');
  });
});
