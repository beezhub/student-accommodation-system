/**
 * The buildings picked on an application form, as its links to the Documents page carry them
 * (#443): `/documents?buildings=3,7`. The page then offers the required document types of those
 * buildings' companies, which the form asks for before any application exists.
 */
export const PICKED_BUILDINGS_PARAM = 'buildings';

/** One application form takes at most three buildings, and the backend refuses more. */
const MAX_BUILDINGS = 3;

/** The building ids in the query parameter, without repeats or anything that is not an id. */
export function pickedBuildingIds(value: string | null): number[] {
  if (!value) {
    return [];
  }
  const ids = value
    .split(',')
    .map((part) => Number(part.trim()))
    .filter((id) => Number.isInteger(id) && id > 0);
  return [...new Set(ids)].slice(0, MAX_BUILDINGS);
}
