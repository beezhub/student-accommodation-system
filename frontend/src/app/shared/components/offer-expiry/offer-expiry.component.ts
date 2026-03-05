import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AllocationStatus } from '../../../core/models/status.model';

export type OfferUrgencyLevel = 'normal' | 'warning' | 'critical' | 'alarm' | 'expired';

@Component({
  selector: 'app-offer-expiry',
  templateUrl: './offer-expiry.component.html',
  styleUrls: ['./offer-expiry.component.scss'],
  standalone: true,
  imports: [CommonModule, MatIconModule],
})
export class OfferExpiryComponent implements OnInit, OnDestroy {
  @Input() expiryDate: string | null = null;
  @Input() status: AllocationStatus | string = '';

  urgencyLevel: OfferUrgencyLevel = 'normal';
  label = '';
  icon = 'schedule';

  private intervalId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.updateUrgency();
    this.intervalId = setInterval(() => this.updateUrgency(), 60_000);
  }

  ngOnDestroy(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
    }
  }

  updateUrgency(): void {
    if (this.status === AllocationStatus.EXPIRED || !this.expiryDate) {
      this.urgencyLevel = 'expired';
      this.label = 'This offer has expired';
      this.icon = 'timer_off';
      return;
    }

    const remainingMs = new Date(this.expiryDate).getTime() - Date.now();

    if (remainingMs <= 0) {
      this.urgencyLevel = 'expired';
      this.label = 'This offer has expired';
      this.icon = 'timer_off';
    } else if (remainingMs <= 60 * 60 * 1000) {
      // < 1 hour
      const minutes = Math.ceil(remainingMs / (60 * 1000));
      this.urgencyLevel = 'alarm';
      this.label = `Expires in ${minutes} minute${minutes !== 1 ? 's' : ''}`;
      this.icon = 'alarm';
    } else if (remainingMs <= 24 * 60 * 60 * 1000) {
      // < 24 hours
      const hours = Math.ceil(remainingMs / (60 * 60 * 1000));
      this.urgencyLevel = 'critical';
      this.label = `Expires in ${hours} hour${hours !== 1 ? 's' : ''} — respond now`;
      this.icon = 'timer';
    } else if (remainingMs <= 3 * 24 * 60 * 60 * 1000) {
      // 1–3 days
      const days = Math.ceil(remainingMs / (24 * 60 * 60 * 1000));
      this.urgencyLevel = 'warning';
      this.label = `Expires in ${days} day${days !== 1 ? 's' : ''} — respond soon`;
      this.icon = 'warning';
    } else {
      // > 3 days
      const days = Math.ceil(remainingMs / (24 * 60 * 60 * 1000));
      this.urgencyLevel = 'normal';
      this.label = `Offer expires in ${days} day${days !== 1 ? 's' : ''}`;
      this.icon = 'schedule';
    }
  }
}
