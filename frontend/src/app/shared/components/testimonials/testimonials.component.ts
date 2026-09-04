import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

interface Testimonial {
  quote: string;
  name: string;
  institution: string;
  year: string;
}

@Component({
  selector: 'app-testimonials',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="testimonials-section">
      <div class="section-header">
        <span class="eyebrow">Student voices</span>
        <h2 class="headline">Trusted by students across South Africa</h2>
      </div>

      <div class="testimonials-grid">
        @for (testimonial of testimonials; track testimonial.name) {
        <article class="testimonial-card">
          <div class="quote-block">
            <mat-icon class="quote-icon">format_quote</mat-icon>
            <p class="quote-text">{{ testimonial.quote }}</p>
          </div>

          <div class="author">
            <div class="avatar" [attr.aria-label]="testimonial.name">
              {{ testimonial.name.charAt(0) }}
            </div>
            <div class="author-info">
              <span class="author-name">{{ testimonial.name }}</span>
              <span class="author-detail"
                >{{ testimonial.institution }} &middot; {{ testimonial.year }}</span
              >
            </div>
          </div>
        </article>
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

      .testimonials-section {
        padding: vars.$space-12 vars.$space-6;
        max-width: 1120px;
        margin: 0 auto;
      }

      /* ── Section header ── */

      .section-header {
        text-align: center;
        margin-bottom: vars.$space-10;
      }

      .eyebrow {
        display: inline-block;
        font-family: vars.$font-sans;
        font-size: vars.$font-size-small;
        font-weight: 600;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: vars.$accent-purple-light;
        margin-bottom: vars.$space-2;
      }

      .headline {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-h1;
        font-weight: vars.$font-weight-h1;
        color: vars.$slate-900;
        line-height: 1.2;
        margin: 0;
      }

      /* ── Grid ── */

      .testimonials-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: vars.$space-6;
      }

      /* ── Card ── */

      .testimonial-card {
        background: #ffffff;
        border-radius: vars.$radius-lg;
        padding: vars.$space-8;
        box-shadow: vars.$shadow-subtle;
        border: 1px solid vars.$slate-100;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: box-shadow vars.$motion-fast ease;
      }

      .testimonial-card:hover {
        box-shadow: vars.$shadow-medium;
      }

      /* ── Quote ── */

      .quote-block {
        margin-bottom: vars.$space-6;
      }

      .quote-icon {
        font-size: 28px !important;
        width: 28px !important;
        height: 28px !important;
        color: vars.$slate-200;
        margin-bottom: vars.$space-2;
        transform: scaleX(-1);
      }

      .quote-text {
        font-family: vars.$font-sans;
        font-size: 15px;
        font-weight: vars.$font-weight-body;
        line-height: 1.7;
        color: vars.$slate-700;
        margin: 0;
      }

      /* ── Author ── */

      .author {
        display: flex;
        align-items: center;
        gap: vars.$space-3;
        padding-top: vars.$space-5;
        border-top: 1px solid vars.$slate-100;
      }

      .avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: vars.$slate-800;
        color: vars.$slate-100;
        display: flex;
        align-items: center;
        justify-content: center;
        font-family: vars.$font-sans;
        font-size: vars.$font-size-body;
        font-weight: 600;
        flex-shrink: 0;
        user-select: none;
      }

      .author-info {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
      }

      .author-name {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-body;
        font-weight: 600;
        color: vars.$slate-900;
        line-height: 1.3;
      }

      .author-detail {
        font-family: vars.$font-sans;
        font-size: vars.$font-size-tiny;
        font-weight: vars.$font-weight-body;
        color: vars.$slate-500;
        line-height: 1.3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      /* ── Responsive ── */

      @media (max-width: vars.$breakpoint-desktop) {
        .testimonials-grid {
          grid-template-columns: 1fr;
          gap: vars.$space-5;
        }

        .testimonials-section {
          padding: vars.$space-10 vars.$space-5;
        }

        .headline {
          font-size: vars.$font-size-h2;
        }
      }
    `,
  ],
})
export class TestimonialsComponent {
  readonly testimonials: Testimonial[] = [
    {
      quote:
        'BeeZhub made finding student housing so much easier. I had my room confirmed within a week of applying.',
      name: 'Thabo M.',
      institution: 'University of Johannesburg',
      year: '3rd Year',
    },
    {
      quote:
        'The application process was straightforward and I always knew where things stood. No more stressing about accommodation.',
      name: 'Naledi K.',
      institution: 'University of Cape Town',
      year: '2nd Year',
    },
    {
      quote:
        'I loved being able to browse buildings and compare options before applying. It felt like I was in control of the process.',
      name: 'James P.',
      institution: 'Stellenbosch University',
      year: '1st Year',
    },
  ];
}
