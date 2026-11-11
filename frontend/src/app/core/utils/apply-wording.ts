import { Application } from '../models/application.model';
import { ExistingApplicationLink, IntakeOption } from '../models/apply-options.model';
import { parseLocalDate } from './local-date';

/**
 * Plain words for intakes and applications on the student screens (GH-495). Students never see
 * "academic year" or "current": "Semester 2, 2027. Move in 15 July. Apply by 30 June."
 */

/** "15 July", or "15 July 2027" when `withYear`. API dates are calendar days, read as local. */
export function formatDay(value: string | null | undefined, withYear = false): string {
  if (!value) {
    return '';
  }
  const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'long' };
  if (withYear) {
    options.year = 'numeric';
  }
  return parseLocalDate(value).toLocaleDateString('en-ZA', options);
}

/** "Semester 2, 2027". */
export function intakeName(intake: Pick<IntakeOption, 'intakeName' | 'academicYearName'>): string {
  return `${intake.intakeName}, ${intake.academicYearName}`;
}

/** "Semester 2, 2027. Move in 15 July. Apply by 30 June." */
export function describeIntake(intake: IntakeOption): string {
  return (
    `${intakeName(intake)}. Move in ${formatDay(intake.stayStartDate)}. ` +
    `Apply by ${formatDay(intake.applicationCloseDate)}.`
  );
}

/** "Company B opens applications on 1 May." */
export function describeOpensOn(companyName: string, next: IntakeOption): string {
  return `${companyName} opens applications on ${formatDay(next.applicationOpenDate)}.`;
}

/** "You already applied for 2027 here (Annual, under review)." */
export function describeExisting(link: ExistingApplicationLink): string {
  const year = link.academicYearName ? ` for ${link.academicYearName}` : '';
  const details = [link.intakeName, statusWords(link.status)].filter(Boolean).join(', ');
  return `You already applied${year} here (${details}).`;
}

/** "Company B needs proof of address and ID Document." */
export function describeMissingDocuments(companyName: string, documents: string[]): string {
  return `${companyName} needs ${joinWords(documents.map(lowerFirst))}.`;
}

/**
 * "2 applications sent: Company A (Residence 1, Residence 3) and Company B (Residence 2)."
 * Buildings come from each application's own preferences, in the student's order.
 */
export function describeSubmitted(applications: Application[]): string {
  const parts = applications.map((app) => {
    const buildings = (app.preferredBuildings ?? [])
      .slice()
      .sort((a, b) => a.preferenceOrder - b.preferenceOrder)
      .map((p) => p.buildingName);
    const names = buildings.length ? buildings : [app.preferredBuildingName].filter(Boolean);
    const company = app.companyName ?? 'Your application';
    return names.length ? `${company} (${names.join(', ')})` : company;
  });
  const lead =
    applications.length === 1 ? 'Application sent' : `${applications.length} applications sent`;
  return `${lead}: ${joinWords(parts)}.`;
}

/** "Annual, 2027" for an application card, or the year it was made for older rows. */
export function applicationIntakeLabel(app: Application): string {
  if (app.intakeName && app.academicYearName) {
    return `${app.intakeName}, ${app.academicYearName}`;
  }
  if (app.academicYearName) {
    return app.academicYearName;
  }
  const date = app.applicationDate || app.createdAt;
  return date ? String(parseLocalDate(date.substring(0, 10)).getFullYear()) : '';
}

/** "UNDER_REVIEW" as "under review". */
export function statusWords(status: string | null | undefined): string {
  return (status ?? '').toLowerCase().replace(/_/g, ' ');
}

/** "a", "a and b", "a, b and c". */
export function joinWords(items: string[]): string {
  if (items.length <= 1) {
    return items.join('');
  }
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}

/** "Proof of address" as "proof of address"; "ID Document" stays, being an abbreviation. */
function lowerFirst(text: string): string {
  if (text.length > 1 && text[1] === text[1].toUpperCase() && text[1] !== text[1].toLowerCase()) {
    return text;
  }
  return text.charAt(0).toLowerCase() + text.slice(1);
}
