import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { RoomManagementComponent } from './room-management.component';
import { RoomResponse, RoomService } from '../../../core/services/room.service';
import { BuildingService } from '../../../core/services/building.service';

describe('RoomManagementComponent', () => {
  const room101 = { id: 1, roomNumber: '101' } as RoomResponse;
  const room102 = { id: 2, roomNumber: '102' } as RoomResponse;

  function setUp(confirmed = true) {
    const snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(confirmed) } as never);
    const roomService = jasmine.createSpyObj<RoomService>('RoomService', [
      'deleteRoom',
      'searchRooms',
    ]);
    roomService.searchRooms.and.returnValue(
      of({ content: [room101, room102], totalElements: 2 }) as never
    );
    const buildingService = jasmine.createSpyObj<BuildingService>('BuildingService', [
      'getAllBuildingsNoPagination',
    ]);
    buildingService.getAllBuildingsNoPagination.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [RoomManagementComponent],
      providers: [
        provideNoopAnimations(),
        { provide: RoomService, useValue: roomService },
        { provide: BuildingService, useValue: buildingService },
      ],
    });
    // The component imports these modules itself, so the spies have to be overrides.
    TestBed.overrideProvider(MatDialog, { useValue: dialog });
    TestBed.overrideProvider(MatSnackBar, { useValue: snackBar });
    const component = TestBed.createComponent(RoomManagementComponent).componentInstance;
    return { component, snackBar, roomService, buildingService };
  }

  describe('deleteRoom', () => {
    it('deleteRoom_refusedByTheServer_showsTheServerMessage', () => {
      const message = 'Room 101 cannot be deleted while it has offered students (1).';
      const { component, snackBar, roomService } = setUp();
      roomService.deleteRoom.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { errors: [message] } }))
      );

      component.deleteRoom(room101);

      expect(snackBar.open).toHaveBeenCalledWith(message, 'Close', jasmine.any(Object));
    });

    it('deleteRoom_noServerMessage_showsTheFallback', () => {
      const { component, snackBar, roomService } = setUp();
      roomService.deleteRoom.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0 }))
      );

      component.deleteRoom(room101);

      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to delete room. It may have active allocations.',
        'Close',
        jasmine.any(Object)
      );
    });

    it('deleteRoom_succeeds_confirmsAndReloads', () => {
      const { component, snackBar, roomService } = setUp();
      roomService.deleteRoom.and.returnValue(of(undefined));

      component.deleteRoom(room101);

      expect(snackBar.open).toHaveBeenCalledWith(
        'Room deleted successfully',
        'Close',
        jasmine.any(Object)
      );
      expect(roomService.searchRooms).toHaveBeenCalled();
    });

    it('deleteRoom_notConfirmed_doesNotCallTheServer', () => {
      const { component, roomService } = setUp(false);

      component.deleteRoom(room101);

      expect(roomService.deleteRoom).not.toHaveBeenCalled();
    });
  });

  describe('loading', () => {
    it('ngOnInit_serverAnswers_loadsRoomsAndTotal', () => {
      const { component } = setUp();

      component.ngOnInit();

      expect(component.rooms).toEqual([room101, room102]);
      expect(component.totalElements).toBe(2);
      expect(component.isLoading).toBeFalse();
    });

    it('loadRooms_serverFails_showsAMessageAndStopsLoading', () => {
      const { component, snackBar, roomService } = setUp();
      roomService.searchRooms.and.returnValue(throwError(() => new Error('down')));

      component.loadRooms();

      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to load rooms',
        'Close',
        jasmine.any(Object)
      );
      expect(component.isLoading).toBeFalse();
    });

    it('loadBuildings_serverFails_keepsTheEmptyList', () => {
      const { component, buildingService } = setUp();
      buildingService.getAllBuildingsNoPagination.and.returnValue(
        throwError(() => new Error('down'))
      );

      component.loadBuildings();

      expect(component.buildings).toEqual([]);
    });
  });

  describe('filters and paging', () => {
    it('onFilter_laterPage_goesBackToTheFirstPage', () => {
      const { component, roomService } = setUp();
      component.currentPage = 3;

      component.onFilter();

      expect(component.currentPage).toBe(0);
      expect(roomService.searchRooms).toHaveBeenCalled();
    });

    it('onClearFilters_filterSet_clearsItAndGoesBackToTheFirstPage', () => {
      const { component } = setUp();
      component.filterForm.patchValue({ roomNumber: '101' });
      component.currentPage = 3;
      expect(component.activeFilterCount).toBe(1);

      component.onClearFilters();

      expect(component.activeFilterCount).toBe(0);
      expect(component.currentPage).toBe(0);
    });

    it('onPageChange_newPage_loadsThatPage', () => {
      const { component, roomService } = setUp();

      component.onPageChange({ pageIndex: 2, pageSize: 50 } as PageEvent);

      expect(roomService.searchRooms).toHaveBeenCalledWith(
        jasmine.objectContaining({ page: 2, size: 50 })
      );
    });

    it('toggleFilters_called_flipsVisibility', () => {
      const { component } = setUp();

      component.toggleFilters();

      expect(component.filtersVisible).toBeTrue();
    });
  });

  describe('display helpers', () => {
    it('getOccupancyText_missingAndPresentValues_formatsBoth', () => {
      const { component } = setUp();

      expect(component.getOccupancyText({} as RoomResponse)).toBe('- / -');
      expect(component.getOccupancyText({ currentOccupancy: 1, capacity: 2 } as RoomResponse)).toBe(
        '1 / 2'
      );
    });

    it('getOccupancyClass_eachOccupancyLevel_returnsItsClass', () => {
      const { component } = setUp();
      const occupancy = (currentOccupancy: number, capacity: number) =>
        component.getOccupancyClass({ currentOccupancy, capacity } as RoomResponse);

      expect(component.getOccupancyClass({} as RoomResponse)).toBe('empty');
      expect(occupancy(0, 0)).toBe('empty');
      expect(occupancy(0, 2)).toBe('empty');
      expect(occupancy(1, 2)).toBe('partial');
      expect(occupancy(2, 2)).toBe('full');
    });

    it('getYearRestrictionTooltip_eachLimit_describesIt', () => {
      const { component } = setUp();
      const tooltip = (minYearOfStudy?: number, maxYearOfStudy?: number) =>
        component.getYearRestrictionTooltip({ minYearOfStudy, maxYearOfStudy } as RoomResponse);

      expect(tooltip(1, 3)).toBe('Year 1 - 3 students only');
      expect(tooltip(2)).toBe('Year 2+ students only');
      expect(tooltip(undefined, 3)).toBe('Up to Year 3 students only');
      expect(tooltip()).toBe('Year of Study Restriction');
    });
  });

  describe('selection', () => {
    it('toggleAllRows_calledTwice_selectsAllThenNone', () => {
      const { component } = setUp();
      component.rooms = [room101, room102];

      component.toggleAllRows();
      expect(component.isAllSelected()).toBeTrue();
      expect(component.isSomeSelected()).toBeFalse();

      component.toggleAllRows();
      expect(component.selection.selected).toEqual([]);
    });

    it('clearSelection_oneRoomSelected_clearsIt', () => {
      const { component } = setUp();
      component.rooms = [room101, room102];
      component.selection.select(room101);
      expect(component.isSomeSelected()).toBeTrue();

      component.clearSelection();

      expect(component.selection.selected).toEqual([]);
    });

    it('openBulkAttributesDialog_nothingSelected_asksForASelection', () => {
      const { component, snackBar } = setUp();

      component.openBulkAttributesDialog();

      expect(snackBar.open).toHaveBeenCalledWith(
        'Please select at least one room',
        'Close',
        jasmine.any(Object)
      );
    });
  });
});
