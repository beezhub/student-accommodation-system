import { Component, ChangeDetectionStrategy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface ProcessStep {
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="how-it-works">
      <div class="how-it-works__inner">
        <div class="how-it-works__header">
          <span class="how-it-works__eyebrow">How it works</span>
          <h2 class="how-it-works__headline">Four simple steps to your new home.</h2>
        </div>

        <div class="how-it-works__grid">
          @for (step of steps; track step.title; let i = $index) {
          <div class="step-card">
            <span class="step-card__number">{{ '0' + (i + 1) }}</span>
            <div class="step-card__icon-ring">
              <mat-icon class="step-card__icon">{{ step.icon }}</mat-icon>
            </div>
            <h3 class="step-card__title">{{ step.title }}</h3>
            <p class="step-card__description">{{ step.description }}</p>
          </div>
          }
        </div>

        <div class="data-assurance">
          <mat-icon class="data-assurance__icon">verified_user</mat-icon>
          <p class="data-assurance__text">
            Your documents are encrypted in transit and at rest. We never share your personal data
            with third parties.
          </p>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      @use '../../../../styles/variables' as vars;

      // ── HOST ──────────────────────────────────────────────────────────────────
      :host {
        display: block;
        font-family: vars.$font-sans;
      }

      // ── SECTION ───────────────────────────────────────────────────────────────
      .how-it-works {
        background: vars.$slate-50;
        padding: 0 vars.$space-10;

        @media (max-width: vars.$breakpoint-tablet) {
          padding: 0 vars.$space-5;
        }
      }

      .how-it-works__inner {
        max-width: 1200px;
        margin: 0 auto;
        padding: vars.$space-12 * 1.5 0 vars.$space-12;

        @media (max-width: vars.$breakpoint-tablet) {
          padding: vars.$space-12 0 vars.$space-10;
        }
      }

      // ── HEADER ────────────────────────────────────────────────────────────────
      .how-it-works__header {
        text-align: center;
        margin-bottom: vars.$space-12;

        @media (max-width: vars.$breakpoint-tablet) {
          margin-bottom: vars.$space-10;
        }
      }

      .how-it-works__eyebrow {
        display: block;
        font-size: 0.6875rem;
        font-weight: 700;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: vars.$slate-400;
        margin-bottom: vars.$space-4;
      }

      .how-it-works__headline {
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: vars.$slate-900;
        letter-spacing: -0.03em;
        line-height: 1.15;
        margin: 0;
      }

      // ── STEP GRID ─────────────────────────────────────────────────────────────
      .how-it-works__grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: vars.$space-6;

        @media (max-width: vars.$breakpoint-tablet) {
          grid-template-columns: 1fr;
          gap: vars.$space-5;
        }
      }

      // ── STEP CARD ─────────────────────────────────────────────────────────────
      .step-card {
        position: relative;
        background: white;
        border: 1px solid vars.$slate-200;
        border-radius: vars.$radius-md;
        padding: vars.$space-8 vars.$space-6;
        box-shadow: vars.$shadow-subtle;
        transition: box-shadow vars.$motion-fast ease, border-color vars.$motion-fast ease;

        &:hover {
          box-shadow: vars.$shadow-medium;
          border-color: vars.$slate-300;
        }

        @media (max-width: vars.$breakpoint-tablet) {
          padding: vars.$space-6 vars.$space-5;
        }
      }

      .step-card__number {
        position: absolute;
        top: vars.$space-5;
        right: vars.$space-5;
        font-size: vars.$font-size-tiny;
        font-weight: 700;
        letter-spacing: 0.1em;
        color: vars.$slate-300;
      }

      .step-card__icon-ring {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 44px;
        height: 44px;
        border-radius: 50%;
        background: vars.$slate-100;
        border: 1px solid vars.$slate-200;
        margin-bottom: vars.$space-5;
      }

      .step-card__icon {
        font-size: 22px !important;
        width: 22px !important;
        height: 22px !important;
        color: vars.$slate-600;
      }

      .step-card__title {
        font-size: vars.$font-size-card-title;
        font-weight: vars.$font-weight-card-title;
        color: vars.$slate-900;
        letter-spacing: -0.02em;
        margin: 0 0 vars.$space-2;
        line-height: 1.25;
      }

      .step-card__description {
        font-size: vars.$font-size-body;
        font-weight: vars.$font-weight-body;
        color: vars.$slate-500;
        line-height: 1.65;
        margin: 0;
      }

      // ── DATA ASSURANCE BAR ────────────────────────────────────────────────────
      .data-assurance {
        display: flex;
        align-items: center;
        gap: vars.$space-3;
        margin-top: vars.$space-10;
        padding: vars.$space-5 vars.$space-6;
        background: vars.$slate-100;
        border: 1px solid vars.$slate-200;
        border-radius: vars.$radius-sm;

        @media (max-width: vars.$breakpoint-tablet) {
          flex-direction: column;
          text-align: center;
          padding: vars.$space-5;
          gap: vars.$space-2;
        }
      }

      .data-assurance__icon {
        font-size: 20px !important;
        width: 20px !important;
        height: 20px !important;
        color: vars.$slate-400;
        flex-shrink: 0;
      }

      .data-assurance__text {
        font-size: vars.$font-size-small;
        font-weight: 400;
        color: vars.$slate-500;
        line-height: 1.6;
        margin: 0;
      }
    `,
  ],
})
export class HowItWorksComponent {
  readonly steps: ProcessStep[] = [
    {
      icon: 'person_add',
      title: 'Create Your Account',
      description:
        'Sign up with your email in under a minute. We\u2019ll verify your identity to keep your account secure.',
    },
    {
      icon: 'school',
      title: 'Complete Your Profile',
      description:
        'Tell us about your institution and study year. That\u2019s all we need to get you browsing.',
    },
    {
      icon: 'home',
      title: 'Apply for Housing',
      description:
        'Browse available buildings, choose your preferences, and submit your application with just a few clicks.',
    },
    {
      icon: 'key',
      title: 'Get Your Room',
      description:
        'We\u2019ll review your application and notify you within days. Accept your offer and move in.',
    },
  ];
}
