import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject, of } from 'rxjs';
import { BuildingFormDialogComponent } from './building-form-dialog.component';
import { BuildingService } from '../../../../core/services/building.service';
import { InstitutionService } from '../../../../core/services/institution.service';
import { GeocodingService, ResolvedPlace } from '../../../../core/services/geocoding.service';
import { TenantService } from '../../../../core/services/tenant.service';
import { RoleService } from '../../../../core/services/role.service';

describe('BuildingFormDialogComponent address lookup', () => {
  let fixture: ComponentFixture<BuildingFormDialogComponent>;
  let geocoding: jasmine.SpyObj<GeocodingService>;

  function create(lookupEnabled: boolean): void {
    geocoding = jasmine.createSpyObj<GeocodingService>('GeocodingService', [
      'isEnabled',
      'search',
      'resolve',
      'newSessionToken',
    ]);
    geocoding.isEnabled.and.returnValue(of(lookupEnabled));
    geocoding.search.and.returnValue(of([]));
    geocoding.newSessionToken.and.returnValues('session-1', 'session-2');

    TestBed.configureTestingModule({
      imports: [BuildingFormDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: GeocodingService, useValue: geocoding },
        { provide: BuildingService, useValue: {} },
        { provide: InstitutionService, useValue: { getInstitutions: () => of([]) } },
        { provide: TenantService, useValue: { getActiveTenants: () => of([]) } },
        { provide: RoleService, useValue: { isPlatformAdmin: () => false } },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        { provide: MAT_DIALOG_DATA, useValue: { mode: 'create' } },
      ],
    });
    fixture = TestBed.createComponent(BuildingFormDialogComponent);
    fixture.detectChanges();
  }

  function typeAddress(value: string): void {
    fixture.componentInstance.onAddressInput({ target: { value } } as unknown as Event);
    tick(400);
  }

  it('does not search when lookup is not configured', fakeAsync(() => {
    create(false);

    typeAddress('1 Main Road');

    expect(fixture.componentInstance.addressLookupEnabled).toBeFalse();
    expect(geocoding.search).not.toHaveBeenCalled();
  }));

  it('searches when lookup is configured', fakeAsync(() => {
    create(true);

    typeAddress('1 Main Road');

    expect(geocoding.search).toHaveBeenCalledOnceWith('1 Main Road', 'session-1');
  }));

  it('keeps the picked text as the address and fetches its coordinates', () => {
    create(true);
    geocoding.resolve.and.returnValue(of({ placeId: 'EiQz', latitude: -26.25, longitude: 27.85 }));

    fixture.componentInstance.onSuggestionSelected({
      placeId: 'EiQz',
      displayName: '32 Tsutsube Street, Soweto, South Africa',
    });

    expect(geocoding.resolve).toHaveBeenCalledOnceWith('EiQz', 'session-1');
    expect(fixture.componentInstance.buildingForm.value).toEqual(
      jasmine.objectContaining({
        address: '32 Tsutsube Street, Soweto, South Africa',
        placeId: 'EiQz',
        latitude: -26.25,
        longitude: 27.85,
      })
    );
  });

  it('starts a new session after a pick', fakeAsync(() => {
    create(true);
    geocoding.resolve.and.returnValue(of({ placeId: 'EiQz', latitude: 1, longitude: 2 }));
    fixture.componentInstance.onSuggestionSelected({ placeId: 'EiQz', displayName: 'A' });

    typeAddress('1 Main Road');

    expect(geocoding.search).toHaveBeenCalledOnceWith('1 Main Road', 'session-2');
  }));

  it('ignores coordinates that arrive after the admin typed again', () => {
    create(true);
    const late = new Subject<ResolvedPlace | null>();
    geocoding.resolve.and.returnValue(late);
    const component = fixture.componentInstance;

    component.onSuggestionSelected({ placeId: 'EiQz', displayName: 'A' });
    component.onAddressInput({ target: { value: 'A2' } } as unknown as Event);
    late.next({ placeId: 'EiQz', latitude: 1, longitude: 2 });

    expect(component.buildingForm.value.latitude).toBeNull();
    expect(component.buildingForm.value.placeId).toBe('');
  });

  it('shows a picked suggestion by its text', () => {
    create(true);
    const component = fixture.componentInstance;

    expect(component.displayAddress({ placeId: 'EiQz', displayName: '32 Tsutsube' })).toBe(
      '32 Tsutsube'
    );
    expect(component.displayAddress('typed')).toBe('typed');
    expect(component.displayAddress(null)).toBe('');
  });

  it('leaves the coordinates empty when they cannot be fetched', () => {
    create(true);
    geocoding.resolve.and.returnValue(of(null));

    fixture.componentInstance.onSuggestionSelected({ placeId: 'EiQz', displayName: 'A' });

    expect(fixture.componentInstance.buildingForm.value.latitude).toBeNull();
    expect(fixture.componentInstance.buildingForm.value.address).toBe('A');
  });
});
