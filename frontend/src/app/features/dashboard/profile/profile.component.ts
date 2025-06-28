import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule } from "@angular/router";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { AuthService } from "../../../core/services/auth.service";
import { YearOfStudyService } from "../../../core/services/year-of-study.service";
import { InstitutionService } from "../../../core/services/institution.service";
import { Institution } from "../../../core/models/institution.model";
import { YearOfStudy } from "../../../core/models/year-of-study.model";
import { StudentService } from "../../../core/services/student.service";
import { StudentProfile } from "../../../core/models/student-profile";

@Component({
  selector: "app-profile",
  templateUrl: "./profile.component.html",
  styleUrls: ["./profile.component.css"],
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
})
export class ProfileComponent implements OnInit {
  currentUser: any;
  profileForm: FormGroup;
  isEditing = false;
  isSaving = false;
  institutions: Institution[] = [];
  yearsOfStudy: YearOfStudy[] = [];
  studentProfile: StudentProfile | null = null;

  constructor(
    private authService: AuthService,
    private studentService: StudentService,
    private institutionService: InstitutionService,
    private yearOfStudyService: YearOfStudyService,
    private fb: FormBuilder
  ) {
    this.profileForm = this.fb.group({
      firstName: ["", Validators.required],
      lastName: ["", Validators.required],
      email: ["", [Validators.required, Validators.email]],
      phone: ["", [Validators.required, Validators.pattern("^[0-9]{10}$")]],
      studentNumber: ["", Validators.required],
      institution: ["", Validators.required],
      yearOfStudy: ["", Validators.required],
      dateOfBirth: ["", Validators.required],
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (this.currentUser) {
      this.studentService.getStudentProfile().subscribe({
        next: (profile) => {
          this.studentProfile = {
            ...profile,
            institutionId: String(profile.institutionId),
            yearOfStudyId: String(profile.yearOfStudyId),
          };
      this.profileForm.patchValue({
        firstName: this.currentUser.firstName,
        lastName: this.currentUser.lastName,
        email: this.currentUser.email,
        phone: profile.phoneNumber,
        studentNumber: profile.studentNumber,
        institution: String(profile.institutionId),
        yearOfStudy: String(profile.yearOfStudyId),
        dateOfBirth: profile.dateOfBirth,
      });
       },
        error: (error) => {
          console.error('Error fetching student profile:', error);
        }
      });
    }
    this.institutionService
      .getInstitutions()
      .subscribe((data) => (this.institutions = data));
    this.yearOfStudyService
      .getYearsOfStudy()
      .subscribe((data) => (this.yearsOfStudy = data));
  }
  getInstitutionName(id: string): string {
    const inst = this.institutions.find(i => String(i.id) === String(id));
    return inst ? inst.name : "";
  }

  getYearOfStudyName(id: string): string {
    const year = this.yearsOfStudy.find(y => String(y.id) === String(id));
    return year ? year.yearOfStudy : "";
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.profileForm.patchValue(this.currentUser);
    }
  }

  onSubmit() {
    if (this.profileForm.valid) {
      this.isSaving = true;
      // Here you would typically update the profile through your service
      setTimeout(() => {
        this.isSaving = false;
        this.isEditing = false;
        // Update the current user with the new values
        this.currentUser = { ...this.currentUser, ...this.profileForm.value };
      }, 1000);
    }
  }
}
