import { BuildingResponse } from '../../../../core/services/building.service';
import { CompanyApplyOptions } from '../../../../core/models/apply-options.model';
import {
  CompanyCheck,
  groupByCompany,
  intakeIdsOf,
  intakeToKeep,
  isReady,
  problemsOf,
} from './company-checks';

describe('company-checks', () => {
  const b = (id: number, tenantId: number) =>
    ({ id, tenantId, companyName: `Company ${tenantId}`, name: `R${id}` } as BuildingResponse);
  const intake = (intakeId: number) => ({
    intakeId,
    intakeName: 'Annual',
    academicYearId: 1,
    academicYearName: '2027',
    stayStartDate: '2027-02-01',
    stayEndDate: '2027-11-30',
    applicationOpenDate: '2026-07-01',
    applicationCloseDate: '2027-01-31',
  });
  const opts = (extra: Partial<CompanyApplyOptions> = {}): CompanyApplyOptions => ({
    companyId: 1,
    companyName: 'Company 1',
    openIntakes: [intake(5)],
    existingApplications: [],
    missingDocuments: [],
    ...extra,
  });
  const check = (extra: Partial<CompanyCheck> = {}): CompanyCheck => ({
    companyId: 1,
    companyName: 'Company 1',
    buildingIds: [1],
    loading: false,
    options: opts(),
    loadError: null,
    chosenIntakeId: 5,
    ...extra,
  });

  it('groupByCompany_keepsThePickOrderAndEarlierChecks', () => {
    const previous = [check({ companyId: 2, chosenIntakeId: 9 })];

    const checks = groupByCompany([3, 1, 4], [b(1, 1), b(3, 2), b(4, 1)], previous);

    expect(checks.map((c) => [c.companyId, c.buildingIds])).toEqual([
      [2, [3]],
      [1, [1, 4]],
    ]);
    expect(checks[0].chosenIntakeId).toBe(9);
    expect(checks[1].options).toBeNull();
  });

  it('intakeToKeep_chosenStillOpen_elseTheOnlyOne_elseNone', () => {
    expect(intakeToKeep(opts({ openIntakes: [intake(5), intake(6)] }), 6)).toBe(6);
    expect(intakeToKeep(opts(), 6)).toBe(5);
    expect(intakeToKeep(opts({ openIntakes: [intake(5), intake(6)] }), null)).toBeNull();
  });

  it('problemsOf_nothingOpenAndNoNextWindow_saysSo', () => {
    expect(problemsOf(check({ options: opts({ openIntakes: [] }) }))).toEqual([
      'Company 1 is not taking applications from your institution right now.',
    ]);
  });

  it('problemsOf_loadFailed_showsTheError', () => {
    expect(problemsOf(check({ options: null, loadError: 'Oops' }))).toEqual(['Oops']);
  });

  it('isReady_needsOptionsNoProblemAndAnIntake', () => {
    expect(isReady(check())).toBeTrue();
    expect(isReady(check({ chosenIntakeId: null }))).toBeFalse();
    expect(isReady(check({ loading: true }))).toBeFalse();
    expect(isReady(check({ options: opts({ missingDocuments: ['ID'] }) }))).toBeFalse();
  });

  it('intakeIdsOf_byCompany', () => {
    expect(intakeIdsOf([check(), check({ companyId: 2, chosenIntakeId: null })])).toEqual({ 1: 5 });
  });
});
