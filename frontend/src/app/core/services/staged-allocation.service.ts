import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  StagedAllocation,
  BatchAllocationProposal,
  BatchConfirmationResult,
  WaitingListEntry,
} from '../models/allocation.models';
import { RoomAllocationResponse } from './room-allocation.service';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({
  providedIn: 'root',
})
export class StagedAllocationService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/admin/allocation`;

  proposeAllocation(studentIds: number[], batchId: string): Observable<BatchAllocationProposal> {
    return this.http.post<BatchAllocationProposal>(
      `${this.apiUrl}/propose-batch`,
      {
        studentIds,
        batchId,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
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

    return this.http.get<PageResponse<StagedAllocation>>(`${this.apiUrl}/staged`, { params });
  }

  getExistingProposals(
    status: string = 'PROPOSED',
    page: number = 0,
    size: number = 100
  ): Observable<PageResponse<StagedAllocation>> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<StagedAllocation>>(`${this.apiUrl}/staged`, { params });
  }

  confirmAllocation(stagedId: number): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/staged/${stagedId}/confirm`,
      {},
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  rejectAllocation(stagedId: number, reason: string): Observable<{ success: boolean }> {
    return this.http.post<{ success: boolean }>(
      `${this.apiUrl}/staged/${stagedId}/reject`,
      {
        reason,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  confirmBatch(batchId: string): Observable<BatchConfirmationResult> {
    return this.http.post<BatchConfirmationResult>(
      `${this.apiUrl}/batch/${batchId}/confirm-all`,
      {},
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  moveStudent(
    allocationId: number,
    newRoomId: number,
    reason: string
  ): Observable<RoomAllocationResponse> {
    return this.http.post<RoomAllocationResponse>(
      `${this.apiUrl}/${allocationId}/move`,
      {
        newRoomId,
        reason,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  overrideConstraints(
    studentId: number,
    roomId: number,
    reason: string,
    bypassConstraints: string[]
  ): Observable<StagedAllocation> {
    return this.http.post<StagedAllocation>(
      `${this.apiUrl}/override`,
      {
        studentId,
        roomId,
        reason,
        bypassConstraints,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  getWaitingList(buildingId?: number): Observable<WaitingListEntry[]> {
    let params = new HttpParams();
    if (buildingId) {
      params = params.set('buildingId', buildingId.toString());
    }
    return this.http.get<WaitingListEntry[]>(`${this.apiUrl}/waiting-list`, {
      params,
      headers: new HttpHeaders({ 'X-Silent-Error': 'true' }),
    });
  }

  rescoreWaitingList(): Observable<{ success: boolean; rescored: number }> {
    return this.http.post<{ success: boolean; rescored: number }>(
      `${this.apiUrl}/waiting-list/rescore`,
      {},
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }

  enqueueWaitingList(studentId: number, applicationId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/waiting-list/enqueue`,
      {
        studentId,
        applicationId,
      },
      { headers: new HttpHeaders({ 'X-Silent-Error': 'true' }) }
    );
  }
}
