export enum ApplicationStatus {
  PENDING = 'PENDING',
  UNDER_REVIEW = 'UNDER_REVIEW',
  CHANGES_REQUESTED = 'CHANGES_REQUESTED',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  AWAITING_ALLOCATION = 'AWAITING_ALLOCATION',
  ALLOCATED = 'ALLOCATED',
  CHECKED_IN = 'CHECKED_IN',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

/** Readable label per application status, matching the backend `ApplicationStatus` labels. */
export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = {
  [ApplicationStatus.PENDING]: 'Pending',
  [ApplicationStatus.UNDER_REVIEW]: 'Under Review',
  [ApplicationStatus.CHANGES_REQUESTED]: 'Changes Requested',
  [ApplicationStatus.APPROVED]: 'Approved',
  [ApplicationStatus.REJECTED]: 'Rejected',
  [ApplicationStatus.AWAITING_ALLOCATION]: 'Awaiting Allocation',
  [ApplicationStatus.ALLOCATED]: 'Allocated',
  [ApplicationStatus.CHECKED_IN]: 'Checked In',
  [ApplicationStatus.EXPIRED]: 'Expired',
  [ApplicationStatus.CANCELLED]: 'Cancelled',
};

/** Application count per status, as the admin stats endpoints return it: every status present. */
export type ApplicationStatusCounts = Record<ApplicationStatus, number>;

/** Counts with every status at zero, for a screen still waiting on the backend. */
export function emptyApplicationStatusCounts(): ApplicationStatusCounts {
  return Object.fromEntries(
    Object.values(ApplicationStatus).map((status) => [status, 0])
  ) as ApplicationStatusCounts;
}

/**
 * The frontend copy of `ApplicationStatus.canTransitionTo` on the backend. Keep the two identical:
 * a button offered for a move the backend refuses only ever ends in an error for the admin.
 */
const APPLICATION_TRANSITIONS: Record<ApplicationStatus, readonly ApplicationStatus[]> = {
  [ApplicationStatus.PENDING]: [
    ApplicationStatus.UNDER_REVIEW,
    ApplicationStatus.APPROVED,
    ApplicationStatus.REJECTED,
    ApplicationStatus.CANCELLED,
  ],
  [ApplicationStatus.UNDER_REVIEW]: [
    ApplicationStatus.PENDING,
    ApplicationStatus.CHANGES_REQUESTED,
    ApplicationStatus.APPROVED,
    ApplicationStatus.REJECTED,
  ],
  [ApplicationStatus.CHANGES_REQUESTED]: [ApplicationStatus.PENDING, ApplicationStatus.CANCELLED],
  [ApplicationStatus.APPROVED]: [
    ApplicationStatus.AWAITING_ALLOCATION,
    ApplicationStatus.ALLOCATED,
    ApplicationStatus.REJECTED,
    ApplicationStatus.CANCELLED,
  ],
  [ApplicationStatus.AWAITING_ALLOCATION]: [
    ApplicationStatus.ALLOCATED,
    ApplicationStatus.REJECTED,
    ApplicationStatus.EXPIRED,
    ApplicationStatus.CANCELLED,
  ],
  [ApplicationStatus.ALLOCATED]: [
    ApplicationStatus.CHECKED_IN,
    ApplicationStatus.AWAITING_ALLOCATION,
    ApplicationStatus.EXPIRED,
    ApplicationStatus.CANCELLED,
  ],
  [ApplicationStatus.CHECKED_IN]: [],
  [ApplicationStatus.REJECTED]: [],
  [ApplicationStatus.EXPIRED]: [],
  [ApplicationStatus.CANCELLED]: [],
};

/** Whether an application in `from` may move to `to`. Staying put does not count as a move. */
export function canApplicationTransition(
  from: ApplicationStatus | string | undefined,
  to: ApplicationStatus
): boolean {
  return APPLICATION_TRANSITIONS[from as ApplicationStatus]?.includes(to) ?? false;
}

export enum AllocationStatus {
  OFFERED = 'OFFERED',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  EXPIRED = 'EXPIRED',
  CHECK_IN_EXPIRED = 'CHECK_IN_EXPIRED',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  TERMINATED = 'TERMINATED',
  CANCELLED = 'CANCELLED',
}
