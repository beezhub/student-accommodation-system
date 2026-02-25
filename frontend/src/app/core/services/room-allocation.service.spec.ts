import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  RoomAllocationService,
  RoomAllocationResponse,
  DeclineOfferRequest,
  DeclineReason,
  PageResponse,
  RoomAllocationRequest,
  AllocateByApplicationRequest,
  RoomAvailabilityResponse,
  AllocationEligibilityResponse,
} from './room-allocation.service';
import { environment } from '../../../environments/environment';

describe('RoomAllocationService', () => {
  let service: RoomAllocationService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiUrl}/room-allocation`;

  const mockAllocation: RoomAllocationResponse = {
    id: 1,
    studentId: 10,
    studentFirstName: 'Jane',
    studentLastName: 'Smith',
    studentEmail: 'jane@example.com',
    roomId: 5,
    roomNumber: '101',
    buildingName: 'Block A',
    allocationDate: '2026-01-01',
    status: 'OFFER_PENDING',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
  };

  const mockPage: PageResponse<RoomAllocationResponse> = {
    content: [mockAllocation],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RoomAllocationService],
    });
    service = TestBed.inject(RoomAllocationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAllAllocations', () => {
    it('getAllAllocations_defaultParams_includesPageAndSize', () => {
      let result: PageResponse<RoomAllocationResponse> | undefined;

      service.getAllAllocations().subscribe((r) => (result = r));

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPage);

      expect(result).toEqual(mockPage);
    });

    it('getAllAllocations_customPage_sendsCorrectParams', () => {
      service.getAllAllocations(2, 10).subscribe();

      const req = httpMock.expectOne((r) => r.url === apiUrl);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPage);
    });
  });

  describe('acceptOffer', () => {
    it('acceptOffer_validId_postsToAcceptEndpoint', () => {
      const checkInDate = '2026-03-01';
      let result: RoomAllocationResponse | undefined;

      service.acceptOffer(1, checkInDate).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1/accept`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ expectedCheckInDate: checkInDate });
      req.flush({ ...mockAllocation, status: 'ACCEPTED' });

      expect(result?.status).toBe('ACCEPTED');
    });
  });

  describe('declineOffer', () => {
    it('declineOffer_withReason_postsToDeclineEndpoint', () => {
      const declineRequest: DeclineOfferRequest = {
        declineReason: DeclineReason.TOO_EXPENSIVE,
      };
      let result: RoomAllocationResponse | undefined;

      service.declineOffer(1, declineRequest).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1/decline`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(declineRequest);
      req.flush({ ...mockAllocation, status: 'DECLINED' });

      expect(result?.status).toBe('DECLINED');
    });

    it('declineOffer_withReasonText_includesTextInBody', () => {
      const declineRequest: DeclineOfferRequest = {
        declineReason: DeclineReason.OTHER,
        declineReasonText: 'Custom reason',
      };

      service.declineOffer(1, declineRequest).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/decline`);
      expect(req.request.body.declineReasonText).toBe('Custom reason');
      req.flush(mockAllocation);
    });
  });

  describe('checkIn', () => {
    it('checkIn_validId_postsToCheckInEndpoint', () => {
      let result: RoomAllocationResponse | undefined;

      service.checkIn(1).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1/check-in`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ ...mockAllocation, status: 'CHECKED_IN' });

      expect(result?.status).toBe('CHECKED_IN');
    });
  });

  describe('getAllocationById', () => {
    it('getAllocationById_validId_returnsAllocation', () => {
      let result: RoomAllocationResponse | undefined;

      service.getAllocationById(1).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAllocation);

      expect(result).toEqual(mockAllocation);
    });
  });

  describe('createAllocation', () => {
    it('createAllocation_validRequest_postsToBaseUrl', () => {
      const request = { studentId: 10, roomId: 5 };

      service.createAllocation(request).subscribe();

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockAllocation);
    });
  });

  describe('endAllocation', () => {
    it('endAllocation_validId_patchesToEndEndpoint', () => {
      service.endAllocation(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1/end`);
      expect(req.request.method).toBe('PATCH');
      req.flush(mockAllocation);
    });
  });

  describe('deleteAllocation', () => {
    it('deleteAllocation_validId_deletesFromBaseUrl', () => {
      service.deleteAllocation(1).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('getAllAllocationsNoPagination', () => {
    it('getAllAllocationsNoPagination_getsFromAllEndpoint', () => {
      let result: RoomAllocationResponse[] | undefined;

      service.getAllAllocationsNoPagination().subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/all`);
      expect(req.request.method).toBe('GET');
      req.flush([mockAllocation]);

      expect(result).toEqual([mockAllocation]);
    });
  });

  describe('searchAllocations', () => {
    it('searchAllocations_emptyFilters_usesDefaultPageParams', () => {
      service.searchAllocations({}).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPage);
    });

    it('searchAllocations_withStudentId_appendsStudentIdParam', () => {
      service.searchAllocations({ studentId: 5 }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('studentId')).toBe('5');
      req.flush(mockPage);
    });

    it('searchAllocations_withAllFilters_sendsAllParams', () => {
      service.searchAllocations({
        studentId: 1,
        roomId: 2,
        buildingId: 3,
        fromDate: '2026-01-01',
        toDate: '2026-12-31',
        studentName: 'Jane',
        roomNumber: '101',
        status: 'ACTIVE',
        page: 1,
        size: 5,
      }).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/search`);
      expect(req.request.params.get('studentId')).toBe('1');
      expect(req.request.params.get('roomId')).toBe('2');
      expect(req.request.params.get('buildingId')).toBe('3');
      expect(req.request.params.get('fromDate')).toBe('2026-01-01');
      expect(req.request.params.get('toDate')).toBe('2026-12-31');
      expect(req.request.params.get('studentName')).toBe('Jane');
      expect(req.request.params.get('roomNumber')).toBe('101');
      expect(req.request.params.get('status')).toBe('ACTIVE');
      req.flush(mockPage);
    });
  });

  describe('getAllocationsByStatus', () => {
    it('getAllocationsByStatus_validStatus_getsFromStatusEndpoint', () => {
      service.getAllocationsByStatus('ACTIVE').subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/status/ACTIVE`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPage);
    });

    it('getAllocationsByStatus_customPageAndSize_sendsCorrectParams', () => {
      service.getAllocationsByStatus('PENDING', 2, 10).subscribe();

      const req = httpMock.expectOne((r) => r.url === `${apiUrl}/status/PENDING`);
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockPage);
    });
  });

  describe('getActiveAllocationByStudentId', () => {
    it('getActiveAllocationByStudentId_validStudentId_getsFromActiveEndpoint', () => {
      let result: RoomAllocationResponse | undefined;

      service.getActiveAllocationByStudentId(10).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/student/10/active`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAllocation);

      expect(result).toEqual(mockAllocation);
    });
  });

  describe('allocateByApplication', () => {
    it('allocateByApplication_validRequest_postsToAllocateEndpoint', () => {
      const request: AllocateByApplicationRequest = { applicationId: 7, roomId: 3 };
      let result: RoomAllocationResponse | undefined;

      service.allocateByApplication(request).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/allocate-by-application`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockAllocation);

      expect(result).toEqual(mockAllocation);
    });
  });

  describe('updateAllocation', () => {
    it('updateAllocation_validRequest_putsToIdEndpoint', () => {
      const request: RoomAllocationRequest = { studentId: 10, roomId: 5 };
      let result: RoomAllocationResponse | undefined;

      service.updateAllocation(1, request).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockAllocation);

      expect(result).toEqual(mockAllocation);
    });
  });

  describe('getRoomAvailability', () => {
    it('getRoomAvailability_validRoomId_getsFromAvailabilityEndpoint', () => {
      const mockAvailability: RoomAvailabilityResponse = {
        roomId: 5,
        roomNumber: '101',
        buildingName: 'Block A',
        capacity: 2,
        currentOccupancy: 1,
        availableSpots: 1,
        isAvailable: true,
        status: 'AVAILABLE',
      };

      let result: RoomAvailabilityResponse | undefined;

      service.getRoomAvailability(5).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/room/5/availability`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAvailability);

      expect(result).toEqual(mockAvailability);
    });
  });

  describe('isRoomAvailable', () => {
    it('isRoomAvailable_availableRoom_returnsTrue', () => {
      let result: boolean | undefined;

      service.isRoomAvailable(5).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/room/5/is-available`);
      expect(req.request.method).toBe('GET');
      req.flush(true);

      expect(result).toBeTrue();
    });
  });

  describe('checkStudentEligibility', () => {
    it('checkStudentEligibility_validStudentId_getsFromEligibilityEndpoint', () => {
      const mockEligibility: AllocationEligibilityResponse = {
        studentId: 10,
        isEligible: true,
        hasApprovedApplication: true,
        hasActiveAllocation: false,
      };

      let result: AllocationEligibilityResponse | undefined;

      service.checkStudentEligibility(10).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/student/10/eligibility`);
      expect(req.request.method).toBe('GET');
      req.flush(mockEligibility);

      expect(result).toEqual(mockEligibility);
    });
  });

  describe('checkOut', () => {
    it('checkOut_validId_postsToCheckOutEndpoint', () => {
      let result: RoomAllocationResponse | undefined;

      service.checkOut(1).subscribe((r) => (result = r));

      const req = httpMock.expectOne(`${apiUrl}/1/check-out`);
      expect(req.request.method).toBe('POST');
      req.flush({ ...mockAllocation, status: 'CHECKED_OUT' });

      expect(result?.status).toBe('CHECKED_OUT');
    });
  });
});
