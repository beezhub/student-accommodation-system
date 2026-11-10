import { CompanyApplyOptions } from '../../../../core/models/apply-options.model';
import { BuildingResponse } from '../../../../core/services/building.service';
import {
  describeExisting,
  describeMissingDocuments,
  describeOpensOn,
} from '../../../../core/utils/apply-wording';

/**
 * One company's part of the form (GH-495): the buildings picked there, what it can take, and the
 * intake chosen. The form checks each company as its buildings are picked, so problems show
 * before submit, and submit sends one intake per company.
 */
export interface CompanyCheck {
  companyId: number;
  companyName: string;
  buildingIds: number[];
  loading: boolean;
  options: CompanyApplyOptions | null;
  loadError: string | null;
  chosenIntakeId: number | null;
}

/**
 * The picked buildings grouped by company, in the order the student picked them. A company
 * already checked keeps its options and chosen intake.
 */
export function groupByCompany(
  selectedIds: number[],
  buildings: BuildingResponse[],
  previous: CompanyCheck[]
): CompanyCheck[] {
  const checks: CompanyCheck[] = [];
  for (const id of selectedIds) {
    const building = buildings.find((b) => b.id === id);
    if (!building || building.tenantId == null) {
      continue;
    }
    let check = checks.find((c) => c.companyId === building.tenantId);
    if (!check) {
      const before = previous.find((c) => c.companyId === building.tenantId);
      check = before
        ? { ...before, buildingIds: [] }
        : {
            companyId: building.tenantId,
            companyName: building.companyName ?? 'This company',
            buildingIds: [],
            loading: false,
            options: null,
            loadError: null,
            chosenIntakeId: null,
          };
      checks.push(check);
    }
    check.buildingIds.push(id);
  }
  return checks;
}

/** The intake to keep after (re)loading options: the one chosen if still open, else the only one. */
export function intakeToKeep(options: CompanyApplyOptions, chosen: number | null): number | null {
  if (chosen !== null && options.openIntakes.some((i) => i.intakeId === chosen)) {
    return chosen;
  }
  return options.openIntakes.length === 1 ? options.openIntakes[0].intakeId : null;
}

/** Why the company cannot be applied to yet, in plain words; empty when it can. */
export function problemsOf(check: CompanyCheck): string[] {
  const options = check.options;
  if (!options) {
    return check.loadError ? [check.loadError] : [];
  }
  const problems: string[] = [];
  if (options.openIntakes.length === 0) {
    if (options.existingApplications.length > 0) {
      problems.push(...options.existingApplications.map(describeExisting));
    } else if (options.nextIntake) {
      problems.push(describeOpensOn(check.companyName, options.nextIntake));
    } else {
      problems.push(
        `${check.companyName} is not taking applications from your institution right now.`
      );
    }
  }
  if (options.missingDocuments.length > 0) {
    problems.push(describeMissingDocuments(check.companyName, options.missingDocuments));
  }
  return problems;
}

/** Whether the company is checked, has no problem and has an intake chosen. */
export function isReady(check: CompanyCheck): boolean {
  return (
    !check.loading &&
    !!check.options &&
    problemsOf(check).length === 0 &&
    check.chosenIntakeId !== null
  );
}

/** The intake chosen per company, as the submit request carries it. */
export function intakeIdsOf(checks: CompanyCheck[]): Record<number, number> {
  const ids: Record<number, number> = {};
  for (const check of checks) {
    if (check.chosenIntakeId !== null) {
      ids[check.companyId] = check.chosenIntakeId;
    }
  }
  return ids;
}
