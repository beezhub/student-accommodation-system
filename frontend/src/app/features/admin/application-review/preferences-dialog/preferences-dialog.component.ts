import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { ApplicationResponse } from '../../../../core/services/admin.service';

export interface PreferencesDialogData {
  application: ApplicationResponse;
}

@Component({
  selector: 'app-preferences-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
  ],
  template: `
    <div class="dialog-header">
      <div class="header-content">
        <h2 mat-dialog-title>Application Preferences</h2>
        <div class="student-subtitle">
          <span class="code">{{ data.application.applicationCode }}</span>
          <span class="separator">•</span>
          <span class="name">{{ data.application.studentFirstName }} {{ data.application.studentLastName }}</span>
        </div>
      </div>
      <button mat-icon-button (click)="close()" class="close-btn">
        <mat-icon>close</mat-icon>
      </button>
    </div>

    <mat-dialog-content class="dialog-content">
      <div class="preferences-container">
        <!-- Accommodation Preferences Section -->
        <div class="section-title">Accommodation Preferences</div>
        <div class="grid-layout">
          <div class="info-card">
            <div class="icon-wrapper primary">
              <mat-icon>apartment</mat-icon>
            </div>
            <div class="info-content">
              <span class="label">Preferred Building</span>
              <span class="value">{{ data.application.preferredBuildingName || 'Any Building' }}</span>
            </div>
          </div>
        </div>

        <mat-divider class="section-divider"></mat-divider>

        <!-- Academic Info Section -->
        <div class="section-title">Academic Information</div>
        <div class="grid-layout">
          <div class="info-card">
            <div class="icon-wrapper info">
              <mat-icon>school</mat-icon>
            </div>
            <div class="info-content">
              <span class="label">Course Name</span>
              <span class="value">{{ data.application.courseName || 'Not specified' }}</span>
            </div>
          </div>

          @if (data.application.allocationScore !== undefined && data.application.allocationScore !== null) {
            <div class="info-card score-card">
              <div class="icon-wrapper purple">
                <mat-icon>speed</mat-icon>
              </div>
              <div class="info-content">
                <span class="label">Allocation Score</span>
                <span class="value score-badge"
                      [class.high]="data.application.allocationScore >= 80"
                      [class.medium]="data.application.allocationScore >= 50 && data.application.allocationScore < 80"
                      [class.low]="data.application.allocationScore < 50">
                  {{ data.application.allocationScore | number:'1.0-0' }}
                </span>
              </div>
            </div>
          }
        </div>

        <!-- Accessibility Section -->
        @if (data.application.requiresAccessibility) {
          <mat-divider class="section-divider"></mat-divider>
          <div class="accessibility-section">
            <div class="section-title accessibility">
              <mat-icon>accessible</mat-icon>
              Accessibility Required
            </div>
            <div class="accessibility-box">
              This applicant requires an accessible room (wheelchair accessible, mobility support, etc.)
            </div>
          </div>
        }

        <!-- Special Requirements Section -->
        @if (data.application.specialRequirements) {
          <mat-divider class="section-divider"></mat-divider>
          <div class="special-req-section">
            <div class="section-title alert">
              <mat-icon>notes</mat-icon>
              Special Requirements
            </div>
            <div class="req-box">
              {{ data.application.specialRequirements }}
            </div>
          </div>
        }
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-stroked-button (click)="close()">Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    :host {
      display: block;
      min-width: 400px;
      max-width: 100vw;
    }

    .dialog-header {
      padding: 16px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      border-bottom: 1px solid rgba(0,0,0,0.08);

      .header-content {
        h2 {
          margin: 0;
          font-size: 20px;
          font-weight: 600;
          color: #1f2937;
          font-family: 'Inter', sans-serif;
        }

        .student-subtitle {
          margin-top: 4px;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          color: #6b7280;

          .code {
            font-family: 'JetBrains Mono', monospace;
            background: #f3f4f6;
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 11px;
            color: #4b5563;
          }
          
          .separator { color: #d1d5db; }
          .name { font-weight: 500; }
        }
      }

      .close-btn {
        margin-top: -8px;
        margin-right: -8px;
        color: #9ca3af;
        
        &:hover { color: #374151; }
      }
    }

    .dialog-content {
      padding: 16px !important;
      max-height: 70vh;
    }

    .preferences-container {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .section-title {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 1px;
      font-weight: 600;
      color: #9ca3af;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 8px;

      &.alert { color: #ef4444; }
    }

    .section-divider {
      border-top-color: rgba(0,0,0,0.06);
    }

    .grid-layout {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
    }

    .info-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px;
      border-radius: 8px;
      background: #ffffff;
      transition: all 0.2s;
      border: 1px solid transparent;

      &:hover {
        background: #f9fafb;
        border-color: #e5e7eb;
      }

      .icon-wrapper {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        mat-icon { font-size: 18px; width: 18px; height: 18px; }

        &.primary { background: #eff6ff; color: #3b82f6; }
        &.accent { background: #fdf2f8; color: #db2777; }
        &.warn { background: #fff1f2; color: #e11d48; }
        &.success { background: #f0fdf4; color: #16a34a; }
        &.info { background: #f0f9ff; color: #0891b2; }
        &.purple { background: #faf5ff; color: #9333ea; }
      }

      .info-content {
        display: flex;
        flex-direction: column;
        gap: 0;
        overflow: hidden;

        .label {
          font-size: 11px;
          color: #6b7280;
          font-weight: 500;
        }

        .value {
          font-size: 13px;
          color: #111827;
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }

    .score-badge {
      display: inline-flex;
      padding: 1px 6px;
      border-radius: 999px;
      font-size: 12px !important;
      
      &.high { background: #dcfce7; color: #15803d; }
      &.medium { background: #fef3c7; color: #b45309; }
      &.low { background: #fee2e2; color: #b91c1c; }
    }

    .accessibility-section {
      background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
      border-radius: 8px;
      padding: 12px;
      border: 1px solid #93c5fd;

      .accessibility-box {
        color: #1e40af;
        font-size: 13px;
        line-height: 1.4;
        font-weight: 500;
      }
    }

    .section-title.accessibility {
      color: #2563eb;
    }

    .special-req-section {
      background: #fef2f2;
      border-radius: 8px;
      padding: 12px;
      border: 1px solid #fee2e2;

      .req-box {
        color: #7f1d1d;
        font-size: 13px;
        line-height: 1.4;
      }
    }

    .dialog-actions {
      padding: 12px 16px;
      border-top: 1px solid rgba(0,0,0,0.08);
      background: #f9fafb;
    }
  `]
})
export class PreferencesDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<PreferencesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PreferencesDialogData
  ) {}

  formatDate(date: string | undefined): string {
    if (!date) return 'Not specified';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
