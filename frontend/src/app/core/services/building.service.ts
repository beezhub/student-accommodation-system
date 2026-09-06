import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
export { PageResponse } from '../models/page-response.model';
import { PageResponse } from '../models/page-response.model';

export interface BuildingRequest {
  name: string;
  address?: string;
  placeId?: string;
  latitude?: number;
  longitude?: number;
  description?: string;
  totalFloors?: number;
  institutionIds?: number[];
  /**
   * The property company that will own the building. Sent only by a platform admin, whose own
   * tenant is the platform sentinel and cannot own buildings; a tenant admin's company is derived
   * from their token (GH-213).
   */
  tenantId?: number;
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

  // Pricing and availability summary (computed from rooms)
  minMonthlyRate?: number | null;
  maxMonthlyRate?: number | null;
  availableRoomCount?: number;

  // Room summaries (included in detail endpoint)
  rooms?: RoomSummary[];

  // Building attributes (included in detail endpoint)
  accessibilityFeatures?: string | null;
}

export interface RoomSummary {
  id: number;
  roomNumber: string;
  capacity: number;
  currentOccupancy: number;
  monthlyRate?: number | null;
  status: string;
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

  browseBuildingsPaginated(filters: {
    minPrice?: number;
    maxPrice?: number;
    available?: boolean;
    institutionId?: number;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<BuildingResponse>> {
    let params = new HttpParams()
      .set('page', (filters.page || 0).toString())
      .set('size', (filters.size || 12).toString());

    if (filters.minPrice != null) params = params.set('minPrice', filters.minPrice.toString());
    if (filters.maxPrice != null) params = params.set('maxPrice', filters.maxPrice.toString());
    if (filters.available != null) params = params.set('available', filters.available.toString());
    if (filters.institutionId != null)
      params = params.set('institutionId', filters.institutionId.toString());
    if (filters.search) params = params.set('search', filters.search);

    return this.http.get<PageResponse<BuildingResponse>>(`${this.apiUrl}/browse`, { params });
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
