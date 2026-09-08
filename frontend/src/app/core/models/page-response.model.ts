export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * A list endpoint's response, which is a page from most endpoints and a bare array from the few
 * that were never paginated.
 *
 * <p>Several screens hedge against both shapes. They did it by casting to `any` and reaching for
 * `.content`, which silences the compiler on the whole object rather than on the one uncertainty
 * (GH-158). This names the uncertainty instead, so the rest of the object stays checked.
 */
export type PageOrArray<T> = PageResponse<T> | T[];

/** The items in a list response, whichever of the two shapes came back. */
export function contentOf<T>(response: PageOrArray<T> | null | undefined): T[] {
  if (!response) {
    return [];
  }
  return Array.isArray(response) ? response : response.content ?? [];
}
