import { Application } from '../models/application.model';
import { IntakeOption } from '../models/apply-options.model';
import {
  applicationIntakeLabel,
  describeExisting,
  describeIntake,
  describeMissingDocuments,
  describeOpensOn,
  describeSubmitted,
  joinWords,
} from './apply-wording';

describe('apply-wording', () => {
  const semester2: IntakeOption = {
    intakeId: 5,
    intakeName: 'Semester 2',
    academicYearId: 2,
    academicYearName: '2027',
    stayStartDate: '2027-07-15',
    stayEndDate: '2027-11-30',
    applicationOpenDate: '2027-05-01',
    applicationCloseDate: '2027-06-30',
  };

  it('describeIntake_plainWords', () => {
    expect(describeIntake(semester2)).toBe('Semester 2, 2027. Move in 15 July. Apply by 30 June.');
  });

  it('describeOpensOn_namesTheCompanyAndTheDay', () => {
    expect(describeOpensOn('Company B', semester2)).toBe('Company B opens applications on 1 May.');
  });

  it('describeExisting_yearIntakeAndStatus', () => {
    expect(
      describeExisting({
        applicationId: 1,
        applicationCode: 'APP-1',
        academicYearName: '2027',
        intakeName: 'Annual',
        status: 'UNDER_REVIEW',
      })
    ).toBe('You already applied for 2027 here (Annual, under review).');
  });

  it('describeMissingDocuments_keepsAbbreviations', () => {
    expect(describeMissingDocuments('Company B', ['Proof of address'])).toBe(
      'Company B needs proof of address.'
    );
    expect(describeMissingDocuments('Company B', ['ID Document', 'Proof of address'])).toBe(
      'Company B needs ID Document and proof of address.'
    );
  });

  it('describeSubmitted_twoCompanies_listsEachWithItsBuildingsInOrder', () => {
    const apps = [
      {
        companyName: 'Company A',
        preferredBuildings: [
          { buildingId: 3, buildingName: 'Residence 3', preferenceOrder: 2 },
          { buildingId: 1, buildingName: 'Residence 1', preferenceOrder: 1 },
        ],
      },
      {
        companyName: 'Company B',
        preferredBuildings: [{ buildingId: 2, buildingName: 'Residence 2', preferenceOrder: 1 }],
      },
    ] as Application[];

    expect(describeSubmitted(apps)).toBe(
      '2 applications sent: Company A (Residence 1, Residence 3) and Company B (Residence 2).'
    );
  });

  it('describeSubmitted_one_usesThePreferredBuilding', () => {
    const apps = [
      { companyName: 'Company A', preferredBuildingName: 'Residence 1' },
    ] as Application[];

    expect(describeSubmitted(apps)).toBe('Application sent: Company A (Residence 1).');
  });

  it('applicationIntakeLabel_intakeAndYear_elseTheYearApplied', () => {
    expect(
      applicationIntakeLabel({ intakeName: 'Annual', academicYearName: '2027' } as Application)
    ).toBe('Annual, 2027');
    expect(applicationIntakeLabel({ academicYearName: '2026' } as Application)).toBe('2026');
    expect(applicationIntakeLabel({ applicationDate: '2025-03-01' } as Application)).toBe('2025');
    expect(applicationIntakeLabel({} as Application)).toBe('');
  });

  it('joinWords', () => {
    expect(joinWords([])).toBe('');
    expect(joinWords(['a'])).toBe('a');
    expect(joinWords(['a', 'b', 'c'])).toBe('a, b and c');
  });
});
