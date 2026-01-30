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
        text-transform: capitalize;
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

      /* Success/Approved - Fresh Green */
      .status-approved {
        background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
        color: #047857;
        border: 1.5px solid #6ee7b7;
      }

      .status-approved:hover {
        background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
      }

      /* Verified - Teal */
      .status-verified {
        background: linear-gradient(135deg, #ccfbf1 0%, #99f6e4 100%);
        color: #0f766e;
        border: 1.5px solid #5eead4;
      }

      .status-verified:hover {
        background: linear-gradient(135deg, #99f6e4 0%, #5eead4 100%);
      }

      /* Pending - Warm Amber */
      .status-pending {
        background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
        color: #b45309;
        border: 1.5px solid #fcd34d;
      }

      .status-pending:hover {
        background: linear-gradient(135deg, #fde68a 0%, #fcd34d 100%);
      }

      /* Rejected - Soft Red */
      .status-rejected {
        background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
        color: #b91c1c;
        border: 1.5px solid #f87171;
      }

      .status-rejected:hover {
        background: linear-gradient(135deg, #fecaca 0%, #fca5a5 100%);
      }

      /* Available - Sky Blue */
      .status-available {
        background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
        color: #0369a1;
        border: 1.5px solid #7dd3fc;
      }

      .status-available:hover {
        background: linear-gradient(135deg, #bae6fd 0%, #7dd3fc 100%);
      }

      /* Occupied - Purple */
      .status-occupied {
        background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
        color: #6d28d9;
        border: 1.5px solid #a78bfa;
      }

      .status-occupied:hover {
        background: linear-gradient(135deg, #ddd6fe 0%, #c4b5fd 100%);
      }

      /* Full - Orange */
      .status-full {
        background: linear-gradient(135deg, #ffedd5 0%, #fed7aa 100%);
        color: #c2410c;
        border: 1.5px solid #fdba74;
      }

      .status-full:hover {
        background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
      }

      /* Maintenance - Gray */
      .status-maintenance {
        background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
        color: #374151;
        border: 1.5px solid #d1d5db;
      }

      .status-maintenance:hover {
        background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
      }

      /* Active - Cyan */
      .status-active {
        background: linear-gradient(135deg, #cffafe 0%, #a5f3fc 100%);
        color: #0e7490;
        border: 1.5px solid #22d3ee;
      }

      .status-active:hover {
        background: linear-gradient(135deg, #a5f3fc 0%, #67e8f9 100%);
      }

      /* Inactive - Slate */
      .status-inactive {
        background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
        color: #475569;
        border: 1.5px solid #cbd5e1;
      }

      .status-inactive:hover {
        background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
      }

      /* Allocated - Success Green */
      .status-allocated {
        background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
        color: #047857;
        border: 1.5px solid #6ee7b7;
      }

      .status-allocated:hover {
        background: linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%);
      }

      /* Awaiting Allocation - Blue */
      .status-awaiting_allocation {
        background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
        color: #1e40af;
        border: 1.5px solid #93c5fd;
      }

      .status-awaiting_allocation:hover {
        background: linear-gradient(135deg, #bfdbfe 0%, #93c5fd 100%);
      }

      /* Default */
      .status-default {
        background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
        color: #64748b;
        border: 1.5px solid #e2e8f0;
      }

      .status-default:hover {
        background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
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
