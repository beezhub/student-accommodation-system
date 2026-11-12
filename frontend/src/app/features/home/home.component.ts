import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ContactInfoService } from '../../core/services/contact-info.service';
import { telHref } from '../../core/models/contact-info.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BrandLogoComponent } from '../../shared/components/brand-logo/brand-logo.component';
import { HowItWorksComponent } from '../../shared/components/how-it-works/how-it-works.component';
import { TestimonialsComponent } from '../../shared/components/testimonials/testimonials.component';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [
    AsyncPipe,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    BrandLogoComponent,
    HowItWorksComponent,
    TestimonialsComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  /** Set by APP_SUPPORT_EMAIL and APP_CONTACT_PHONE on the backend. */
  readonly contact$ = inject(ContactInfoService).getContactInfo();
  readonly telHref = telHref;

  readonly stats = [
    { value: '5,000+', label: 'Students housed' },
    { value: '200+', label: 'Rooms available' },
    { value: '48h', label: 'Avg. response time' },
  ] as const;

  readonly features = [
    {
      title: 'Simple Applications',
      description:
        'Submit your housing application in minutes with a guided step-by-step process and instant confirmation.',
    },
    {
      title: 'Room Selection',
      description:
        'Browse available rooms, compare options, and choose the accommodation that fits your needs and budget.',
    },
    {
      title: 'Secure & Reliable',
      description:
        'Your personal data and documents are protected with enterprise-grade security throughout the process.',
    },
  ] as const;
}
