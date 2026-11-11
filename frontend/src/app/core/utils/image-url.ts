import { environment } from '../../../environments/environment';
import { BuildingImageResponse, BuildingResponse } from '../services/building.service';

/**
 * The backend hands back image URLs relative to the API root (`/building/.../content`), not a
 * host-qualified address, the same way every other endpoint is only a path here and gets
 * `environment.apiUrl` prepended. Every service that returns buildings resolves them through
 * these, so each list, browse, detail and application picker response points at wherever this
 * app's own API is, in dev or in prod, instead of a path the browser resolves against the page
 * (GH-268, #540).
 */

/** Prefixes `environment.apiUrl` to a root-relative URL; anything else is left as it is. */
export function resolveImageUrl<T extends string | null | undefined>(url: T): T {
  if (url && url.startsWith('/')) {
    return `${environment.apiUrl}${url}` as T;
  }
  return url;
}

/** The image with its `imageUrl` resolved. */
export function resolveBuildingImageUrls(image: BuildingImageResponse): BuildingImageResponse {
  return { ...image, imageUrl: resolveImageUrl(image.imageUrl) };
}

/** The building with its primary image and every gallery image resolved. */
export function resolveBuildingUrls(building: BuildingResponse): BuildingResponse {
  return {
    ...building,
    primaryImageUrl: resolveImageUrl(building.primaryImageUrl),
    images: building.images?.map(resolveBuildingImageUrls),
  };
}
