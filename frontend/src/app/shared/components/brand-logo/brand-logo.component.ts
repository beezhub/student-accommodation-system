import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  imports: [RouterModule, MatIconModule],
  template: `
    @if (variant === 'toolbar') {
    <a class="brand-link" routerLink="/dashboard">
      <mat-icon class="brand-icon">home_work</mat-icon>
      <div class="brand-text-group">
        <span class="brand-name">BeeZhub</span>
        <span class="brand-tagline">Student Housing Made Easy</span>
      </div>
    </a>
    } @else {
    <div class="sidebar-brand-inner">
      <mat-icon class="brand-icon sidebar-icon">home_work</mat-icon>
      <div class="brand-text-group">
        <span class="brand-name sidebar-name">BeeZhub</span>
        <span class="brand-tagline">Student Housing Made Easy</span>
      </div>
    </div>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        flex-shrink: 0;
      }

      /* ── Toolbar variant ── */
      .brand-link {
        display: flex;
        align-items: center;
        gap: 9px;
        text-decoration: none;
        padding: 6px 10px;
        border-radius: 8px;
        transition: background-color 150ms ease;
        flex-shrink: 0;
      }

      .brand-link:hover {
        background: rgba(255, 255, 255, 0.08);
      }

      /* ── Sidebar variant ── */
      .sidebar-brand-inner {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        height: 68px;
        padding: 0 1.25rem;
        border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      }

      /* ── Shared icon ── */
      .brand-icon {
        font-size: 22px !important;
        width: 22px !important;
        height: 22px !important;
        color: #ffffff;
        flex-shrink: 0;
      }

      .sidebar-icon {
        font-size: 1.625rem !important;
        width: 1.625rem !important;
        height: 1.625rem !important;
        color: #a78bfa;
      }

      /* ── Text group ── */
      .brand-text-group {
        display: flex;
        flex-direction: column;
        gap: 1px;
      }

      .brand-name {
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
        font-size: 17px;
        font-weight: 800;
        letter-spacing: -0.04em;
        color: #ffffff;
        line-height: 1.1;
      }

      .sidebar-name {
        font-size: 1.125rem;
        letter-spacing: -0.02em;
      }

      .brand-tagline {
        font-size: 10px;
        font-weight: 400;
        color: rgba(255, 255, 255, 0.45);
        letter-spacing: 0.01em;
        line-height: 1.2;
      }

      @media (max-width: 768px) {
        .brand-name {
          font-size: 14px;
        }
      }
    `,
  ],
})
export class BrandLogoComponent {
  @Input() variant: 'toolbar' | 'sidebar' = 'toolbar';
}
