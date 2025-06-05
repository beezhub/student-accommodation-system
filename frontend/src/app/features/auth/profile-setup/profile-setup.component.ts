import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { InstitutionService } from '../../../core/services/institution.service';
import { YearOfStudyService} from '../../../core/services/year-of-study.service';
import { StudentService } from '../../../core/services/student.service';
import { Institution } from '../../../core/models/institution.model';
import { YearOfStudy } from '../../../core/models/year-of-study.model';

@Component({
  selector: 'app-profile-setup',
  templateUrl: './profile-setup.component.html',
  styleUrls: ['./profile-setup.component.css'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class ProfileSetupComponent implements OnInit {
  profileForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  institutions: Institution[] = [];
  yearsOfStudy: YearOfStudy[] = [];
  
  
  constructor(
      private fb: FormBuilder,
      private router: Router,
      private authService: AuthService,
      private institutionService: InstitutionService,
      private yearOfStudyService: YearOfStudyService,
      private studentService: StudentService) {

    this.profileForm = this.fb.group({
      studentNumber: ['', [Validators.required]],
      phoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
      dateOfBirth: ['', [Validators.required]],
      institution: ['', [Validators.required]],
      yearOfStudy: ['', [Validators.required]],
      gender: ['', [Validators.required]],
      specialRequirements: ['']
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }

    this.institutionService.getInstitutions().subscribe({
      next: (institutions) => {
        this.institutions = institutions;
      },
      error: (error) => {
        console.error('Error fetching institutions:', error);
      }
    });

    this.yearOfStudyService.getYearsOfStudy().subscribe({
      next: (yearOfStudy) => {
        this.yearsOfStudy = yearOfStudy;
      },
      error: (error) => {
        console.error('Error fetching years of study:', error);
      }
    });
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSubmitting = true;
      this.isSubmitting = false;
      
      const studentData = {
        userId: this.currentUser.id,
        studentNumber: this.profileForm.get('studentNumber')?.value,
        dateOfBirth: this.profileForm.get('dateOfBirth')?.value,
        institutionId: this.profileForm.get('institution')?.value,
        yearOfStudyId: this.profileForm.get('yearOfStudy')?.value,
        gender: this.profileForm.get('gender')?.value,
        specialRequirements: this.profileForm.get('specialRequirements')?.value,
        phoneNumber: this.profileForm.get('phoneNumber')?.value
      }
    

      this.studentService.createStudentProfile(studentData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/document-upload']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error updating profile:', error);
        }
      });
    } else {
      this.profileForm.markAllAsTouched();
    }
  }
}