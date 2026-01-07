import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentData } from '../../../core/models/document.model';
import { DocumentService } from '../../../core/services/document.service';
import { User } from '../../../core/models/user.model';
import { StudentService } from '../../../core/services/student.service';
import { StudentResponse } from '../../../core/models/student.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService } from '../../../core/services/year-of-study.service';
import { Institution } from '../../../core/models/institution.model';
import { ApplicationService } from '../../../core/services/application.service';

@Component({
  selector: 'app-application-review',
  templateUrl: './application-review.component.html',
  styleUrls: ['./application-review.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatChipsModule
  ]
})
export class ApplicationReviewComponent implements OnInit {
  isSubmitting = false;
  termsAccepted = false;
  currentUser: User | null = null;
  studentProfile: StudentResponse | null = null;
  documents: DocumentData[] = [];
  yearOfStudy: YearOfStudy[] =[];
  institutions: Institution[] = [];
  

  constructor(
      private router: Router,
      private authService: AuthService,
      private documentService: DocumentService,
      private studentService: StudentService,
      private yearOfStudyService: YearOfStudyService,
      private institutionService: InstitutionService,
      private ApplicationService: ApplicationService
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.currentUser = this.authService.currentUserValue;
    this.studentService.getStudentProfile().subscribe({
      next: student => {
        this.studentProfile = student;
      },
      error: err => {
        console.error('Failed to load student profile', err);
        this.router.navigate(['/login']);
      }
    });
    

    this.documentService.getUploadedDocuments().subscribe({
      next: docsInfo => this.documents = docsInfo,
      error: err => console.error('Failed to load documents', err)
    });

    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: yearOfStudy => {
        if (yearOfStudy && Array.isArray((yearOfStudy as any).content)) {
          this.yearOfStudy = (yearOfStudy as any).content;
        } else if (Array.isArray(yearOfStudy)) {
          this.yearOfStudy = yearOfStudy;
        } else {
          this.yearOfStudy = [];
        }
      },
      error: err => {
        console.error('Failed to load year of study', err);
      }
    });

    this.institutionService.getInstitutions().subscribe({
      next: institutions => {
        if (institutions && Array.isArray((institutions as any).content)) {
          this.institutions = (institutions as any).content;
        } else if (Array.isArray(institutions)) {
          this.institutions = institutions;
        } else {
          this.institutions = [];
        }
      },
      error: err => {
        console.error('Failed to load institutions', err);
      }
    });


  }

  onSubmit() {
    this.isSubmitting = true;

    // Get the most recent application for this student
    this.ApplicationService.getMyApplications().subscribe({
      next: (applications) => {
        console.log('Applications retrieved:', applications);
        this.isSubmitting = false;

        if (applications && applications.length > 0) {
          // Navigate to the most recent application's confirmation page
          const latestApplication = applications[0];
          this.router.navigate([`/application-confirmation/${latestApplication.id}`]);
        } else {
          // No application found, redirect to dashboard
          console.warn('No application found for student');
          this.router.navigate(['/dashboard']);
        }
      },
      error: err => {
        console.error('Failed to retrieve applications', err);
        this.isSubmitting = false;
        // On error, just navigate to dashboard
        this.router.navigate(['/dashboard']);
      }
    });
  }

  getInstitutionName(): string {
    if (!this.institutions || !this.studentProfile?.institutionId) {
      return 'N/A';
    }
    const institutionId = String(this.studentProfile.institutionId);
    const inst = this.institutions.find(i => String(i.id) === institutionId);
    return inst?.name || 'N/A';
  }

  getYearOfStudyName(): string {
    if (!this.yearOfStudy || !this.studentProfile?.yearOfStudyId) {
      return 'N/A';
    }
    const yearOfStudyId = String(this.studentProfile.yearOfStudyId);
    const year = this.yearOfStudy.find(y => String(y.id) === yearOfStudyId);
    return year?.yearOfStudy || 'N/A'; 
  }
}