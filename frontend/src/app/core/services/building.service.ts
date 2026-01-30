import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BuildingRequest {
  name: string;
  address?: string;
  description?: string;
  totalFloors?: number;
  institutionIds?: number[];
}

export interface InstitutionSummary {
  id: number;
  name: string;
}

export interface BuildingResponse {
  id: number;
  name: string;
  address?: string;
  description?: string;
  totalFloors?: number;
  institutions?: InstitutionSummary[];
  createdAt: string;
  updatedAt: string;
}

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
export class BuildingService {
  private apiUrl = `${environment.apiUrl}/building`;

  constructor(private http: HttpClient) {}

  getAllBuildings(page: number = 0, size: number = 20): Observable<PageResponse<BuildingResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<BuildingResponse>>(this.apiUrl, { params });
  }

  getAllBuildingsNoPagination(): Observable<BuildingResponse[]> {
    return this.http.get<BuildingResponse[]>(`${this.apiUrl}/all`);
  }

  searchBuildings(filters: {
    name?: string;
    address?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<BuildingResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 20).toString());

    if (filters.name) params = params.set('name', filters.name);
    if (filters.address) params = params.set('address', filters.address);

    return this.http.get<PageResponse<BuildingResponse>>(`${this.apiUrl}/search`, { params });
  }

  getBuildingById(id: number): Observable<BuildingResponse> {
    return this.http.get<BuildingResponse>(`${this.apiUrl}/${id}`);
  }

  createBuilding(building: BuildingRequest): Observable<BuildingResponse> {
    return this.http.post<BuildingResponse>(this.apiUrl, building);
  }

  updateBuilding(id: number, building: BuildingRequest): Observable<BuildingResponse> {
    return this.http.put<BuildingResponse>(`${this.apiUrl}/${id}`, building);
  }

  deleteBuilding(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getBuildingsByInstitution(institutionId: number): Observable<BuildingResponse[]> {
    return this.http.get<BuildingResponse[]>(`${this.apiUrl}/by-institution/${institutionId}`);
  }
}
