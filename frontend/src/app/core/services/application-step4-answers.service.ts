import { Injectable } from '@angular/core';
import { parseLocalDate, toLocalDateString } from '../utils/local-date';

/** What step 4 of the application wizard keeps between reloads. The confirmation is not kept. */
export interface Step4Answers {
  studentNumber: string;
  phoneNumber: string;
  dateOfBirth: Date | null;
  gender: string;
  courseName: string;
  requiresAccessibility: boolean;
  preferredBuildingIds: number[];
  specialRequirements: string;
}

type StoredAnswers = Omit<Step4Answers, 'dateOfBirth'> & { dateOfBirth: string | null };

/**
 * Keeps step 4's answers until Continue to Review creates the application, so a refresh does not
 * empty the form (#550). Session storage, not local storage: a phone number and date of birth
 * should not stay on a shared computer once the tab is closed. Keyed by user, so a different
 * student signing in on the same tab never sees them.
 */
@Injectable({ providedIn: 'root' })
export class ApplicationStep4AnswersService {
  private static readonly PREFIX = 'applicationStep4:';

  save(userId: string | number, answers: Step4Answers): void {
    const stored: StoredAnswers = {
      ...answers,
      // A calendar day: JSON.stringify would write the picked local midnight as UTC (#464).
      dateOfBirth: answers.dateOfBirth ? toLocalDateString(answers.dateOfBirth) : null,
    };
    try {
      sessionStorage.setItem(this.key(userId), JSON.stringify(stored));
    } catch {
      // Storage full or blocked: the form still works, it just will not survive a refresh.
    }
  }

  load(userId: string | number): Partial<Step4Answers> | null {
    let raw: string | null;
    try {
      raw = sessionStorage.getItem(this.key(userId));
    } catch {
      return null;
    }
    if (!raw) {
      return null;
    }
    try {
      const stored = JSON.parse(raw) as Partial<StoredAnswers>;
      const { dateOfBirth, ...rest } = stored;
      const answers: Partial<Step4Answers> = { ...rest };
      if (dateOfBirth) {
        answers.dateOfBirth = parseLocalDate(dateOfBirth);
      }
      return answers;
    } catch {
      this.clear(userId);
      return null;
    }
  }

  clear(userId: string | number): void {
    try {
      sessionStorage.removeItem(this.key(userId));
    } catch {
      // Nothing to clear if storage is unavailable.
    }
  }

  private key(userId: string | number): string {
    return `${ApplicationStep4AnswersService.PREFIX}${userId}`;
  }
}
