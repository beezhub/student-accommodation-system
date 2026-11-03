import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Client-side copy of the backend password policy (`StrongPasswordValidator`, GH-118). Keep the
 * two in step: when the form allowed 8 characters but the server wanted 12 plus four character
 * classes, a short password passed the form and came back as a generic signup error.
 */
export const PASSWORD_MIN_LENGTH = 12;
/** BCrypt ignores anything past 72 bytes, so the backend rejects longer passwords. */
export const PASSWORD_MAX_BYTES = 72;

export const PASSWORD_RULES_HINT =
  `At least ${PASSWORD_MIN_LENGTH} characters, with an uppercase letter, ` +
  'a lowercase letter, a number and a symbol';

/**
 * Returns `{ strongPassword: { missing: string[] } }`, where each entry reads after "Password
 * needs", e.g. "an uppercase letter". An empty value passes so `Validators.required` owns that case.
 */
export const strongPasswordValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const value: unknown = control.value;
  if (typeof value !== 'string' || value.length === 0) {
    return null;
  }

  const missing: string[] = [];
  if (value.length < PASSWORD_MIN_LENGTH) {
    missing.push(`at least ${PASSWORD_MIN_LENGTH} characters`);
  }
  if (new TextEncoder().encode(value).length > PASSWORD_MAX_BYTES) {
    missing.push(`no more than ${PASSWORD_MAX_BYTES} characters`);
  }
  if (!/\p{Lu}/u.test(value)) {
    missing.push('an uppercase letter');
  }
  if (!/\p{Ll}/u.test(value)) {
    missing.push('a lowercase letter');
  }
  if (!/\p{Nd}/u.test(value)) {
    missing.push('a number');
  }
  if (!/[^\p{Lu}\p{Ll}\p{Nd}]/u.test(value)) {
    missing.push('a symbol');
  }

  return missing.length > 0 ? { strongPassword: { missing } } : null;
};
