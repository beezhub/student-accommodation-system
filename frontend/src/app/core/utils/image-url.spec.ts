import { environment } from '../../../environments/environment';
import { BuildingImageResponse, BuildingResponse } from '../services/building.service';
import { resolveBuildingImageUrls, resolveBuildingUrls, resolveImageUrl } from './image-url';

describe('image-url', () => {
  const relative = '/building/5/images/1/content';
  const resolved = `${environment.apiUrl}${relative}`;

  const image: BuildingImageResponse = {
    id: 1,
    buildingId: 5,
    fileName: 'front.jpg',
    imageUrl: relative,
    isPrimary: true,
    displayOrder: 0,
    createdAt: '2026-01-01T00:00:00',
  };

  const building: BuildingResponse = {
    id: 5,
    name: 'Residence 1',
    primaryImageUrl: relative,
    images: [image],
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
  };

  it('resolveImageUrl_rootRelative_isPrefixedWithApiUrl', () => {
    expect(resolveImageUrl(relative)).toBe(resolved);
  });

  it('resolveImageUrl_absolute_isUnchanged', () => {
    expect(resolveImageUrl('https://cdn.example.com/a.jpg')).toBe('https://cdn.example.com/a.jpg');
  });

  it('resolveImageUrl_missing_isUnchanged', () => {
    expect(resolveImageUrl(undefined)).toBeUndefined();
    expect(resolveImageUrl(null)).toBeNull();
    expect(resolveImageUrl('')).toBe('');
  });

  it('resolveBuildingImageUrls_resolvesImageUrl_keepsTheRest', () => {
    const result = resolveBuildingImageUrls(image);

    expect(result).toEqual({ ...image, imageUrl: resolved });
    expect(image.imageUrl).toBe(relative);
  });

  it('resolveBuildingUrls_resolvesPrimaryAndGalleryImages', () => {
    const result = resolveBuildingUrls(building);

    expect(result.primaryImageUrl).toBe(resolved);
    expect(result.images?.[0].imageUrl).toBe(resolved);
    expect(result.name).toBe('Residence 1');
    expect(building.primaryImageUrl).toBe(relative);
  });

  it('resolveBuildingUrls_noImages_leavesThemMissing', () => {
    const result = resolveBuildingUrls({
      ...building,
      primaryImageUrl: undefined,
      images: undefined,
    });

    expect(result.primaryImageUrl).toBeUndefined();
    expect(result.images).toBeUndefined();
  });
});
