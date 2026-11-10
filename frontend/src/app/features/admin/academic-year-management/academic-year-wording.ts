import { AcademicYear, Intake } from '../../../core/models/academic-year.model';

/** One institution's years on the admin screen, newest first. */
export interface InstitutionGroup {
  institutionId: number | null;
  label: string;
  years: AcademicYear[];
}

export const ALL_INSTITUTIONS = 'All institutions';

/**
 * Reads an API date (`2027-07-15`) as a local calendar day. `new Date('2027-07-15')` is UTC
 * midnight, which shows as the day before anywhere west of Greenwich.
 */
export function parseApiDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** A local calendar day for the API. Never `toISOString()`, which shifts it to UTC (GH-461). */
export function toApiDate(date: Date | null | undefined): string {
  if (!date) {
    return '';
  }
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** "15 July", or "15 July 2026" when the year differs from `contextYear`. */
export function formatDay(value: string, contextYear?: number): string {
  const date = parseApiDate(value);
  if (!date) {
    return '-';
  }
  const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'long' };
  if (contextYear !== undefined && date.getFullYear() !== contextYear) {
    options.year = 'numeric';
  }
  return date.toLocaleDateString('en-ZA', options);
}

/** "Semester 2, 2027. Move in 15 July. Applications 17 March to 1 July." */
export function describeIntake(intake: Intake): string {
  const year = parseApiDate(intake.stayStartDate)?.getFullYear() ?? 0;
  return (
    `${intake.name}, ${year}. Move in ${formatDay(intake.stayStartDate, year)}. ` +
    `Applications ${formatDay(intake.applicationOpenDate, year)} to ` +
    `${formatDay(intake.applicationCloseDate, year)}.`
  );
}

/** "1 February 2027 to 30 November 2027". */
export function describeYearDates(year: AcademicYear): string {
  return `${formatDay(year.startDate, -1)} to ${formatDay(year.endDate, -1)}`;
}

/** What an edit touched, in plain words, or null when nothing is on file yet. */
export function describeAffected(applications?: number, allocations?: number): string | null {
  if (!applications && !allocations) {
    return null;
  }
  return (
    `${applications ?? 0} application(s) and ${allocations ?? 0} room allocation(s) keep ` +
    'their intake and dates.'
  );
}

/** Years grouped by institution, "All institutions" first, then by name. */
export function groupByInstitution(years: AcademicYear[]): InstitutionGroup[] {
  const groups = new Map<string, InstitutionGroup>();
  for (const year of years) {
    const id = year.institutionId ?? null;
    const key = id === null ? 'all' : String(id);
    let group = groups.get(key);
    if (!group) {
      group = {
        institutionId: id,
        label: id === null ? ALL_INSTITUTIONS : year.institutionName ?? 'Institution',
        years: [],
      };
      groups.set(key, group);
    }
    group.years.push(year);
  }
  const sorted = [...groups.values()].sort((a, b) => {
    if (a.institutionId === null) return -1;
    if (b.institutionId === null) return 1;
    return a.label.localeCompare(b.label);
  });
  sorted.forEach((group) => group.years.sort((a, b) => b.startDate.localeCompare(a.startDate)));
  return sorted;
}
