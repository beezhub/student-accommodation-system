import { GENDER_OPTIONS, normalizeGender } from './gender';

describe('gender', () => {
  it('GENDER_OPTIONS_listed_offersMaleAndFemaleOnly', () => {
    expect(GENDER_OPTIONS.map((o) => o.value)).toEqual(['Male', 'Female']);
  });

  [
    ['male', 'Male'],
    ['MALE', 'Male'],
    [' Female ', 'Female'],
    ['Other', ''],
    ['prefer-not-to-say', ''],
    ['', ''],
    [null, ''],
    [undefined, ''],
  ].forEach(([stored, shown]) => {
    it(`normalizeGender_${stored}_returns_${shown || 'empty'}`, () => {
      expect(normalizeGender(stored)).toBe(shown as string);
    });
  });
});
