import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BuildingRequest {
  name: string;
  address?: string;
  placeId?: string;
  latitude?: number;
  longitude?: number;
  description?: string;
  totalFloors?: number;
  institutionIds?: number[];
}

export interface InstitutionSummary {
  id: number;
  name: string;
}

export interface BuildingImageResponse {
  id: number;
  buildingId: number;
  fileName: string;
  imageUrl: string;
  isPrimary: boolean;
  displayOrder: number;
  createdAt: string;
}

export interface BuildingResponse {
  id: number;
  tenantId?: number;
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  placeId?: string;
  formattedAddress?: string;
  addressComponents?: { [key: string]: string };
  description?: string;
  totalFloors?: number;
  institutions?: InstitutionSummary[];
  primaryImageUrl?: string;
  images?: BuildingImageResponse[];
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

export interface MapsConfig {
  enabled: boolean;
  apiKey: string;
}

@Injectable({
  providedIn: 'root',
})
export class BuildingService {
  private http = inject(HttpClient);

  private apiUrl = `${environment.apiUrl}/building`;

  getMapsConfig(): Observable<MapsConfig> {
    return this.http.get<MapsConfig>(`${this.apiUrl}/maps-config`);
  }

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

  // Building Image Methods

  getBuildingImages(buildingId: number): Observable<BuildingImageResponse[]> {
    return this.http.get<BuildingImageResponse[]>(`${this.apiUrl}/${buildingId}/images`);
  }

  uploadBuildingImages(buildingId: number, files: File[]): Observable<BuildingImageResponse[]> {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    return this.http.post<BuildingImageResponse[]>(`${this.apiUrl}/${buildingId}/images`, formData);
  }

  deleteBuildingImage(buildingId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${buildingId}/images/${imageId}`);
  }

  setPrimaryImage(buildingId: number, imageId: number): Observable<BuildingImageResponse> {
    return this.http.put<BuildingImageResponse>(
      `${this.apiUrl}/${buildingId}/images/${imageId}/primary`,
      {}
    );
  }
}
