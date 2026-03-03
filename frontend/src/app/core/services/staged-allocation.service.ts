import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StagedAllocation,
  BatchAllocationProposal,
  WaitingListEntry,
} from '../models/allocation.models';
import { RoomAllocationResponse } from './room-allocation.service';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class StagedAllocationService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/allocation`;

  proposeAllocation(studentIds: number[], batchId: string): Observable<BatchAllocationProposal> {
    return this.http.post<BatchAllocationProposal>(`${this.apiUrl}/propose-batch`, {
      studentIds,
      batchId,
    });
  }

  getProposals(
    batchId: string,
    status?: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<StagedAllocation>> {
    let params = new HttpParams()
      .set('batchId', batchId)
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PageResponse<StagedAllocation>>(this.apiUrl, { params });
  }

  confirmAllocation(stagedId: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/staged/${stagedId}/confirm`, {});
  }

  rejectAllocation(stagedId: number, reason: string): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(`${this.apiUrl}/staged/${stagedId}/reject`, {
      reason,
    });
  }

  confirmBatch(
    batchId: string
  ): Observable<{ success: boolean; confirmed: number; failed: number }> {
    return this.http.post<{ success: boolean; confirmed: number; failed: number }>(
      `${this.apiUrl}/batch/${batchId}/confirm-all`,
      {}
    );
  }

  moveStudent(
    allocationId: number,
    newRoomId: number,
    reason: string
  ): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(`${this.apiUrl}/${allocationId}/move`, {
      newRoomId,
      reason,
    });
  }

  overrideConstraints(
    studentId: number,
    roomId: number,
    reason: string,
    bypassConstraints: string[]
  ): Observable<StagedAllocation> {
    return this.http.post<StagedAllocation>(`${this.apiUrl}/override`, {
      studentId,
      roomId,
      reason,
      bypassConstraints,
    });
  }

  getWaitingList(buildingId?: number): Observable<WaitingListEntry[]> {
    let params = new HttpParams();
    if (buildingId) {
      params = params.set('buildingId', buildingId.toString());
    }
    return this.http.get<WaitingListEntry[]>(`${this.apiUrl}/waiting-list`, { params });
  }

  rescoreWaitingList(): Observable<{ success: boolean; rescored: number }> {
    return this.http.post<{ success: boolean; rescored: number }>(
      `${this.apiUrl}/waiting-list/rescore`,
      {}
    );
  }

  enqueueWaitingList(studentId: number, applicationId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/waiting-list/enqueue`, {
      studentId,
      applicationId,
    });
  }
}
