import { AcademicYear, Intake, IntakeType } from '../../../core/models/academic-year.model';

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

/** One intake "Create year" will make, before it is saved. */
export interface IntakePreview {
  name: string;
  stayStartDate: string;
  stayEndDate: string;
}

const INTAKE_PARTS: Record<IntakeType, { label: string; parts: number }> = {
  ANNUAL: { label: 'Annual', parts: 1 },
  SEMESTER: { label: 'Semester', parts: 2 },
  TRIMESTER: { label: 'Trimester', parts: 3 },
};

/**
 * The intakes "Create year" will make: Annual is the whole year, Semesters split it in two and
 * Trimesters in three. Mirrors `IntakeService.split` on the server, which stays the source of
 * truth. Empty when the dates are missing, reversed, or too short to split.
 */
export function previewIntakes(start: string, end: string, types: IntakeType[]): IntakePreview[] {
  const first = parseApiDate(start);
  const last = parseApiDate(end);
  if (!first || !last) {
    return [];
  }
  const dayMs = 24 * 60 * 60 * 1000;
  const utc = (date: Date) => Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
  const days = Math.round((utc(last) - utc(first)) / dayMs) + 1;
  const plusDays = (offset: number) =>
    toApiDate(new Date(first.getFullYear(), first.getMonth(), first.getDate() + offset));

  const intakes: IntakePreview[] = [];
  for (const type of types) {
    const { label, parts } = INTAKE_PARTS[type];
    if (days < parts * 2) {
      return [];
    }
    for (let part = 1; part <= parts; part++) {
      intakes.push({
        name: parts === 1 ? label : `${label} ${part}`,
        stayStartDate: plusDays(Math.floor((days * (part - 1)) / parts)),
        stayEndDate: part === parts ? end : plusDays(Math.floor((days * part) / parts) - 1),
      });
    }
  }
  return intakes;
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
