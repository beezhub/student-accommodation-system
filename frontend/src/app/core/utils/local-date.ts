/**
 * A date of birth is a calendar day, with no time and no zone. These two keep it that way between
 * the datepicker, which works in local midnights, and the API, which sends `yyyy-MM-dd` (#464).
 *
 * The built-ins both pass through UTC: `date.toISOString()` turns local midnight on the 14th into
 * the 13th east of Greenwich, and `new Date('2000-05-14')` reads as UTC midnight, the 13th west of
 * it.
 */

/** Reads `yyyy-MM-dd` as local midnight of that day. Anything else is left to `new Date`. */
export function parseLocalDate(value: string): Date {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  return match ? new Date(+match[1], +match[2] - 1, +match[3]) : new Date(value);
}

/**
 * The local calendar day as `yyyy-MM-dd`. Takes a string too, because a draft saved earlier can
 * still hold the full timestamp `JSON.stringify` made from the picked date.
 */
export function toLocalDateString(value: Date | string): string {
  const date = typeof value === 'string' ? parseLocalDate(value) : value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
