import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-owner-email-verify',
  templateUrl: './owner-email-verify.component.html',
  styleUrls: ['./owner-email-verify.component.css'],
  standalone: true,
  imports: [RouterModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OwnerEmailVerifyComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  state: 'loading' | 'success' | 'error' = 'loading';
  errorMessage = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'];
    if (!token) {
      this.state = 'error';
      this.errorMessage = 'No verification token found in the link.';
      this.cdr.markForCheck();
      return;
    }

    this.http
      .get(`${environment.apiUrl}/public/owner/verify-email?token=${encodeURIComponent(token)}`)
      .subscribe({
        next: () => {
          this.state = 'success';
          this.cdr.markForCheck();
          setTimeout(() => this.router.navigate(['/owner/awaiting-approval']), 2000);
        },
        error: (error) => {
          this.state = 'error';
          this.errorMessage =
            error?.status === 400
              ? 'This verification link has expired or is invalid.'
              : 'Verification failed. Please try again.';
          this.cdr.markForCheck();
        },
      });
  }
}
