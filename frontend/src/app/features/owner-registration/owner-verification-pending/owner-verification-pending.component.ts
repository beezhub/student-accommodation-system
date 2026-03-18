import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-owner-verification-pending',
  templateUrl: './owner-verification-pending.component.html',
  styleUrls: ['./owner-verification-pending.component.css'],
  standalone: true,
  imports: [RouterModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OwnerVerificationPendingComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  email = '';
  isResending = false;
  resendCooldown = 0;
  private cooldownInterval?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParams['email'] || '';
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) clearInterval(this.cooldownInterval);
  }

  resendVerification(): void {
    if (this.isResending || this.resendCooldown > 0 || !this.email) return;
    this.isResending = true;
    this.http
      .post(`${environment.apiUrl}/public/owner/resend-verification?email=${encodeURIComponent(this.email)}`, {})
      .subscribe({
        next: () => {
          this.isResending = false;
          this.startCooldown();
          this.snackBar.open('Verification email resent. Please check your inbox.', 'Close', {
            duration: 5000,
            panelClass: ['success-snackbar'],
          });
        },
        error: () => {
          this.isResending = false;
          this.cdr.markForCheck();
          this.snackBar.open('Failed to resend email. Please try again.', 'Close', {
            duration: 4000,
            panelClass: ['error-snackbar'],
          });
        },
      });
  }

  private startCooldown(): void {
    this.resendCooldown = 30;
    this.cdr.markForCheck();
    this.cooldownInterval = setInterval(() => {
      this.resendCooldown--;
      this.cdr.markForCheck();
      if (this.resendCooldown <= 0) {
        clearInterval(this.cooldownInterval);
      }
    }, 1000);
  }
}
