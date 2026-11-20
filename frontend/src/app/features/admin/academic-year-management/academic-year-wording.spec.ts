import { AcademicYear, Intake } from '../../../core/models/academic-year.model';
import {
  describeAffected,
  describeIntake,
  groupByInstitution,
  isPlatformYear,
  ownerLabel,
  parseApiDate,
  previewIntakes,
  toApiDate,
} from './academic-year-wording';

describe('academic-year-wording', () => {
  const semester2: Intake = {
    id: 1,
    academicYearId: 1,
    intakeType: 'SEMESTER',
    name: 'Semester 2',
    stayStartDate: '2027-07-15',
    stayEndDate: '2027-11-30',
    applicationOpenDate: '2027-03-17',
    applicationCloseDate: '2027-07-01',
    isApplicationWindowOpen: false,
  };

  it('describeIntake_readsAsTheIssueWordsIt', () => {
    expect(describeIntake(semester2)).toBe(
      'Semester 2, 2027. Move in 15 July. Applications 17 March to 1 July.'
    );
  });

  it('describeIntake_windowInTheYearBefore_namesThatYear', () => {
    const annual = {
      ...semester2,
      name: 'Annual',
      stayStartDate: '2027-02-01',
      applicationOpenDate: '2026-10-04',
    };

    expect(describeIntake(annual)).toContain('Applications 4 October 2026 to 1 July.');
  });

  it('parseAndFormat_keepTheLocalCalendarDay', () => {
    const date = parseApiDate('2027-07-15');

    expect(date?.getDate()).toBe(15);
    expect(toApiDate(date)).toBe('2027-07-15');
    expect(parseApiDate('')).toBeNull();
    expect(toApiDate(null)).toBe('');
  });

  it('describeAffected_nothingOnFile_isNull', () => {
    expect(describeAffected(0, 0)).toBeNull();
    expect(describeAffected(3, 1)).toContain('3 application(s) and 1 room allocation(s)');
  });

  it('groupByInstitution_putsAllInstitutionsFirstAndNewestYearFirst', () => {
    const year = (
      id: number,
      startDate: string,
      institutionId: number | null,
      institutionName?: string
    ) => ({ id, name: String(id), startDate, institutionId, institutionName } as AcademicYear);

    const groups = groupByInstitution([
      year(1, '2026-02-01', 9, 'Zed College'),
      year(2, '2026-02-01', null),
      year(3, '2027-02-01', null),
      year(4, '2027-02-01', 7, 'Alpha University'),
    ]);

    expect(groups.map((g) => g.label)).toEqual([
      'All institutions',
      'Alpha University',
      'Zed College',
    ]);
    expect(groups[0].years.map((y) => y.id)).toEqual([3, 2]);
  });

  it('previewIntakes_annualAndSemesters_splitsTheYearLikeTheServer', () => {
    expect(previewIntakes('2027-02-01', '2027-11-30', ['ANNUAL', 'SEMESTER'])).toEqual([
      { name: 'Annual', stayStartDate: '2027-02-01', stayEndDate: '2027-11-30' },
      { name: 'Semester 1', stayStartDate: '2027-02-01', stayEndDate: '2027-07-01' },
      { name: 'Semester 2', stayStartDate: '2027-07-02', stayEndDate: '2027-11-30' },
    ]);
  });

  it('previewIntakes_trimesters_coverTheYearInThreeParts', () => {
    expect(previewIntakes('2027-01-15', '2027-12-10', ['TRIMESTER'])).toEqual([
      { name: 'Trimester 1', stayStartDate: '2027-01-15', stayEndDate: '2027-05-04' },
      { name: 'Trimester 2', stayStartDate: '2027-05-05', stayEndDate: '2027-08-22' },
      { name: 'Trimester 3', stayStartDate: '2027-08-23', stayEndDate: '2027-12-10' },
    ]);
  });

  it('previewIntakes_datesMissingReversedOrTooShort_isEmpty', () => {
    expect(previewIntakes('', '2027-11-30', ['ANNUAL'])).toEqual([]);
    expect(previewIntakes('2027-11-30', '2027-02-01', ['ANNUAL'])).toEqual([]);
    expect(previewIntakes('2027-02-01', '2027-02-03', ['SEMESTER'])).toEqual([]);
  });

  describe('ownerLabel', () => {
    const year: AcademicYear = {
      id: 1,
      name: '2027',
      startDate: '2027-02-01',
      endDate: '2027-11-30',
      isApplicationWindowOpen: false,
    };

    it('ownerLabel_platformYear_saysPlatform', () => {
      expect(isPlatformYear({ ...year, tenantId: 0 })).toBeTrue();
      expect(ownerLabel({ ...year, tenantId: 0 })).toBe('Platform');
    });

    it('ownerLabel_companyYear_namesTheCompany', () => {
      const companyYear = { ...year, tenantId: 42, companyName: 'Acme Residences' };
      expect(isPlatformYear(companyYear)).toBeFalse();
      expect(ownerLabel(companyYear)).toBe('Acme Residences');
    });
  });
});
