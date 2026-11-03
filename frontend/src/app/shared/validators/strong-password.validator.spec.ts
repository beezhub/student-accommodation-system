import { FormControl } from '@angular/forms';
import { strongPasswordValidator } from './strong-password.validator';

describe('strongPasswordValidator', () => {
  const missingFor = (value: string): string[] | undefined =>
    strongPasswordValidator(new FormControl(value))?.['strongPassword']?.missing;

  it('validate_emptyValue_leavesItToRequired', () => {
    expect(strongPasswordValidator(new FormControl(''))).toBeNull();
  });

  it('validate_passwordMeetingEveryRule_returnsNull', () => {
    expect(strongPasswordValidator(new FormControl('Correct-Horse9'))).toBeNull();
  });

  it('validate_shortPassword_listsLengthRule', () => {
    expect(missingFor('Ab1!')).toEqual(['at least 12 characters']);
  });

  it('validate_eightLowercaseLetters_listsEveryMissingRule', () => {
    expect(missingFor('password')).toEqual([
      'at least 12 characters',
      'an uppercase letter',
      'a number',
      'a symbol',
    ]);
  });

  it('validate_over72Bytes_listsMaxRule', () => {
    expect(missingFor('Aa1!' + 'a'.repeat(70))).toEqual(['no more than 72 characters']);
  });
});
