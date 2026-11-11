/**
 * The gender a student can choose. Allocation compares it with room and building restrictions
 * without regard to case, so the spelling is only for display; both screens send `Male` or
 * `Female` so a value saved on one shows up selected on the other (#539).
 */
export const GENDER_OPTIONS = [
  { value: 'Male', viewValue: 'Male' },
  { value: 'Female', viewValue: 'Female' },
] as const;

/**
 * Maps a stored gender to an option value. Older profiles can hold `male`, `MALE`, `Other`,
 * `prefer-not-to-say` or nothing; anything that is not male or female comes back empty, so the
 * student is asked again.
 */
export function normalizeGender(gender: string | null | undefined): string {
  const value = gender?.trim().toLowerCase();
  if (value === 'male') return 'Male';
  if (value === 'female') return 'Female';
  return '';
}
