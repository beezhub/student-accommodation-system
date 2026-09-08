import { Injectable } from '@angular/core';

export interface ApplicationDraft {
  studentId: number;
  requiresAccessibility: boolean;
  preferredBuildingIds: number[];
  specialRequirements: string | null;
  institutionName: string;
  courseName: string;
  /** Deferred profile fields saved alongside the draft */
  studentNumber: string;
  phoneNumber: string;
  dateOfBirth: Date | string;
  gender: string;
}

const DRAFT_KEY = 'application_draft';

/**
 * Holds draft application data between the application-submission and
 * application-review wizard steps. Data is persisted to localStorage so
 * it survives page refreshes.
 */
@Injectable({ providedIn: 'root' })
export class ApplicationDraftService {
  saveDraft(draft: ApplicationDraft): void {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  }

  getDraft(): ApplicationDraft | null {
    const raw = localStorage.getItem(DRAFT_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as ApplicationDraft;
    } catch {
      return null;
    }
  }

  clearDraft(): void {
    localStorage.removeItem(DRAFT_KEY);
  }

  hasDraft(): boolean {
    return localStorage.getItem(DRAFT_KEY) !== null;
  }
}
