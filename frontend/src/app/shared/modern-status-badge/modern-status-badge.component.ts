import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';

import { MatIconModule } from '@angular/material/icon';

export type StatusType =
  | 'approved'
  | 'APPROVED'
  | 'pending'
  | 'PENDING'
  | 'rejected'
  | 'REJECTED'
  | 'verified'
  | 'VERIFIED'
  | 'available'
  | 'AVAILABLE'
  | 'occupied'
  | 'OCCUPIED'
  | 'maintenance'
  | 'MAINTENANCE'
  | 'full'
  | 'FULL'
  | 'active'
  | 'ACTIVE'
  | 'inactive'
  | 'INACTIVE'
  | 'allocated'
  | 'ALLOCATED'
  | 'awaiting_allocation'
  | 'AWAITING_ALLOCATION'
  | 'under_review'
  | 'UNDER_REVIEW'
  | 'changes_requested'
  | 'CHANGES_REQUESTED'
  | 'withdrawn'
  | 'WITHDRAWN'
  | 'checked_in'
  | 'CHECKED_IN'
  | 'offered'
  | 'OFFERED'
  | 'accepted'
  | 'ACCEPTED'
  | 'declined'
  | 'DECLINED'
  | 'completed'
  | 'COMPLETED'
  | 'terminated'
  | 'TERMINATED'
  | 'submitted'
  | 'SUBMITTED'
  | string;

interface StatusConfig {
  icon: string;
  label: string;
  colorClass: string;
  emoji?: string;
}

@Component({
  selector: 'app-modern-status-badge',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      [class]="'modern-badge ' + config().colorClass"
      [class.badge-sm]="size() === 'sm'"
      [class.badge-lg]="size() === 'lg'"
      role="status"
      [attr.aria-label]="'Status: ' + config().label"
    >
      @if (showEmoji() && config().emoji) {
      <span class="badge-emoji">{{ config().emoji }}</span>
      } @else {
      <mat-icon class="badge-icon">{{ config().icon }}</mat-icon>
      }
      <span class="badge-label">{{ config().label }}</span>
      @if (pulse()) {
      <span class="badge-pulse"></span>
      }
    </div>
  `,
  styles: [
    `
      .modern-badge {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 8px 16px;
        border-radius: 24px;
        font-weight: 600;
        font-size: 13px;
        letter-spacing: 0.02em;
        position: relative;
        overflow: hidden;
        transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        backdrop-filter: blur(8px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      .modern-badge:hover {
        transform: translateY(-2px) scale(1.02);
        box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
      }

      .badge-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        line-height: 16px;
      }

      .badge-emoji {
        font-size: 14px;
        line-height: 1;
      }

      .badge-label {
      }

      .badge-pulse {
        position: absolute;
        top: 50%;
        right: 12px;
        transform: translateY(-50%);
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: currentColor;
        animation: pulse 2s infinite;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
          transform: translateY(-50%) scale(1);
        }
        50% {
          opacity: 0.5;
          transform: translateY(-50%) scale(1.5);
        }
      }

      /* Size variants */
      .badge-sm {
        padding: 4px 10px;
        font-size: 11px;
        gap: 4px;
      }

      .badge-sm .badge-icon {
        font-size: 14px;
        width: 14px;
        height: 14px;
      }

      .badge-lg {
        padding: 12px 24px;
        font-size: 15px;
        gap: 8px;
      }

      .badge-lg .badge-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      /* Status Colors - Vibrant, Student-Friendly Palette */

      /* Status Colors - Mapped to Functional Palette */

      /* Success/Approved */
      .status-approved,
      .status-verified,
      .status-allocated,
      .status-active {
        background: var(--color-success-bg, linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%));
        color: var(--color-success, #047857);
        border: 1px solid var(--color-success-border, #6ee7b7);
      }

      /* Warning/Pending/Full */
      .status-pending,
      .status-full,
      .status-awaiting_allocation,
      .status-warning {
        background: var(--color-warning-bg, linear-gradient(135deg, #fef3c7 0%, #fde68a 100%));
        color: var(--color-warning, #b45309);
        border: 1px solid var(--color-warning-border, #fcd34d);
      }

      /* Error/Rejected */
      .status-rejected {
        background: var(--color-error-bg, linear-gradient(135deg, #fee2e2 0%, #fecaca 100%));
        color: var(--color-error, #b91c1c);
        border: 1px solid var(--color-error-border, #f87171);
      }

      /* Expired */
      .status-expired {
        background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%);
        color: #c2410c;
        border: 1px solid #fb923c;
      }

      /* Check-In Expired */
      .status-check-in-expired {
        background: linear-gradient(135deg, #fef2f2 0%, #fecaca 100%);
        color: #991b1b;
        border: 1px solid #f87171;
      }

      /* Info/Available/Offered/Submitted */
      .status-available,
      .status-info {
        background: var(--color-info-bg, linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%));
        color: var(--color-info, #0369a1);
        border: 1px solid var(--color-info-border, #7dd3fc);
      }

      /* Neutral/Maintenance/Inactive/Withdrawn/Declined/Completed */
      .status-maintenance,
      .status-inactive,
      .status-occupied,
      .status-neutral {
        background: var(--color-slate-100, #f1f5f9);
        color: var(--color-slate-600, #475569);
        border: 1px solid var(--color-slate-200, #cbd5e1);
      }

      /* Changes Requested */
      .status-changes {
        background: #fff7ed;
        color: #c2410c;
        border: 1px solid #fb923c;
      }

      .status-default {
        background: var(--color-slate-50, #f8fafc);
        color: var(--color-slate-500, #64748b);
        border: 1px solid var(--color-slate-200, #e2e8f0);
      }
    `,
  ],
})
export class ModernStatusBadgeComponent {
  status = input.required<StatusType>();
  size = input<'sm' | 'md' | 'lg'>('md');
  showEmoji = input<boolean>(false);
  pulse = input<boolean>(false);

  config = computed<StatusConfig>(() => {
    const status = this.status();
    const statusLower = (status || 'unknown').toLowerCase();

    const configs: Record<string, StatusConfig> = {
      approved: {
        icon: 'check_circle',
        label: 'Approved',
        colorClass: 'status-approved',
        emoji: '✅',
      },
      pending: {
        icon: 'schedule',
        label: 'Pending',
        colorClass: 'status-pending',
        emoji: '⏳',
      },
      rejected: {
        icon: 'cancel',
        label: 'Rejected',
        colorClass: 'status-rejected',
        emoji: '❌',
      },
      verified: {
        icon: 'verified',
        label: 'Verified',
        colorClass: 'status-verified',
        emoji: '✔️',
      },
      available: {
        icon: 'check_circle_outline',
        label: 'Available',
        colorClass: 'status-available',
        emoji: '🏠',
      },
      occupied: {
        icon: 'person',
        label: 'Occupied',
        colorClass: 'status-occupied',
        emoji: '👤',
      },
      full: {
        icon: 'block',
        label: 'Full',
        colorClass: 'status-full',
        emoji: '🚫',
      },
      maintenance: {
        icon: 'build',
        label: 'Maintenance',
        colorClass: 'status-maintenance',
        emoji: '🔧',
      },
      active: {
        icon: 'radio_button_checked',
        label: 'Active',
        colorClass: 'status-active',
        emoji: '🟢',
      },
      inactive: {
        icon: 'radio_button_unchecked',
        label: 'Inactive',
        colorClass: 'status-inactive',
        emoji: '⚪',
      },
      allocated: {
        icon: 'assignment_turned_in',
        label: 'Allocated',
        colorClass: 'status-allocated',
        emoji: '✅',
      },
      awaiting_allocation: {
        icon: 'hourglass_empty',
        label: 'Awaiting Allocation',
        colorClass: 'status-awaiting_allocation',
        emoji: '⏳',
      },
      expired: {
        icon: 'timer_off',
        label: 'Expired',
        colorClass: 'status-expired',
        emoji: '⏰',
      },
      check_in_expired: {
        icon: 'event_busy',
        label: 'Check-In Expired',
        colorClass: 'status-check-in-expired',
        emoji: '🚫',
      },
      under_review: {
        icon: 'rate_review',
        label: 'Under Review',
        colorClass: 'status-warning',
        emoji: '👀',
      },
      changes_requested: {
        icon: 'edit_note',
        label: 'Changes Requested',
        colorClass: 'status-changes',
        emoji: '⚠️',
      },
      withdrawn: {
        icon: 'undo',
        label: 'Withdrawn',
        colorClass: 'status-neutral',
        emoji: '↩️',
      },
      checked_in: {
        icon: 'meeting_room',
        label: 'Checked In',
        colorClass: 'status-approved',
        emoji: '🔑',
      },
      offered: {
        icon: 'local_offer',
        label: 'Offer Sent',
        colorClass: 'status-info',
        emoji: '📩',
      },
      accepted: {
        icon: 'check_circle',
        label: 'Accepted',
        colorClass: 'status-approved',
        emoji: '✅',
      },
      declined: {
        icon: 'thumb_down',
        label: 'Declined',
        colorClass: 'status-neutral',
        emoji: '👎',
      },
      completed: {
        icon: 'task_alt',
        label: 'Completed',
        colorClass: 'status-neutral',
        emoji: '🏁',
      },
      terminated: {
        icon: 'block',
        label: 'Terminated',
        colorClass: 'status-rejected',
        emoji: '🛑',
      },
      submitted: {
        icon: 'send',
        label: 'Submitted',
        colorClass: 'status-info',
        emoji: '📤',
      },
    };

    return (
      configs[statusLower] || {
        icon: 'info',
        label: this.status(),
        colorClass: 'status-default',
        emoji: '📋',
      }
    );
  });
}
