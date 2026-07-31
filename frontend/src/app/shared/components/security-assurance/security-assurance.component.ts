import { Component, ChangeDetectionStrategy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface AssuranceItem {
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-security-assurance',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="security-assurance">
      <h3 class="section-heading">Your data is safe with us</h3>

      <div class="assurance-grid">
        @for (item of assuranceItems; track item.icon) {
        <div class="assurance-item">
          <div class="icon-circle">
            <mat-icon class="assurance-icon">{{ item.icon }}</mat-icon>
          </div>
          <h4 class="assurance-title">{{ item.title }}</h4>
          <p class="assurance-description">{{ item.description }}</p>
        </div>
        }
      </div>
    </section>
  `,
  styles: [
    `
      @use '../../../../styles/variables' as vars;

      :host {
        display: block;
      }

      .security-assurance {
        padding: vars.$space-8 vars.$space-6;
        background: vars.$slate-50;
        border-top: 1px solid vars.$slate-200;
      }

      .section-heading {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-card-title;
        font-weight: vars.$font-weight-card-title;
        color: vars.$slate-800;
        text-align: center;
        margin: 0 0 vars.$space-8 0;
      }

      .assurance-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: vars.$space-6;
        max-width: 960px;
        margin: 0 auto;
      }

      .assurance-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        padding: vars.$space-4;
      }

      .icon-circle {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: rgba(vars.$success, 0.1);
        margin-bottom: vars.$space-3;
        flex-shrink: 0;
      }

      .assurance-icon {
        color: vars.$success;
        font-size: 24px;
        width: 24px;
        height: 24px;
      }

      .assurance-title {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-body;
        font-weight: 600;
        color: vars.$slate-800;
        margin: 0 0 vars.$space-2 0;
      }

      .assurance-description {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-small;
        font-weight: vars.$font-weight-body;
        color: vars.$slate-500;
        line-height: 1.5;
        margin: 0;
      }

      @media (max-width: vars.$breakpoint-tablet) {
        .assurance-grid {
          grid-template-columns: 1fr;
          gap: vars.$space-6;
          max-width: 480px;
        }
      }
    `,
  ],
})
export class SecurityAssuranceComponent {
  readonly assuranceItems: AssuranceItem[] = [
    {
      icon: 'lock',
      title: 'Secure Document Storage',
      description:
        'Your uploaded documents are stored with enterprise-grade encryption. Only authorised administrators can view your files during the review process.',
    },
    {
      icon: 'shield',
      title: 'Privacy Protected',
      description:
        'We comply with POPIA (Protection of Personal Information Act). Your personal data is never sold or shared with third parties.',
    },
    {
      icon: 'verified_user',
      title: 'Verified Process',
      description:
        "Every application is reviewed by real administrators at your institution. You'll receive email notifications at every stage of the process.",
    },
  ];
}
