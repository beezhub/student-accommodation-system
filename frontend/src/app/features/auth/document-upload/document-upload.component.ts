import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Router } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { DocumentTypeService } from "../../../core/services/document-type.service";
import { DocumentType } from "../../../core/models/document-type.model";
import { DocumentService } from "../../../core/services/document.service";

@Component({
  selector: "app-document-upload",
  templateUrl: "./document-upload.component.html",
  styleUrls: ["./document-upload.component.css"],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
})
export class DocumentUploadComponent implements OnInit {
  uploadForm: FormGroup;
  isSubmitting = false;
  currentUser: any;
  uploadedFiles: { [key: string]: File } = {};

  requiredDocuments: DocumentType[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private documentTypeService: DocumentTypeService,
    private documentService: DocumentService
  ) {
    this.uploadForm = this.fb.group({});
  }

  ngOnInit() {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(["/login"]);
    }
    this.documentTypeService.getRequiredDocumentTypes().subscribe({
      next: (documentTypes: any[]) => {
        this.requiredDocuments = documentTypes;
        documentTypes.forEach((doc) => {
          if (!this.uploadForm.contains(doc.id)) {
            this.uploadForm.addControl(
              doc.id,
              this.fb.control(null, Validators.required)
            );
          }
        });
      },
      error: (err) => {
        console.error("Failed to load required document types", err);
      },
    });
  }

  onFileSelected(event: any, documentId: string) {
    if (event.target.files && event.target.files[0]) {
      const file = event.target.files[0];
      this.uploadedFiles[documentId] = file;
      this.uploadForm.patchValue({ [documentId]: file });
    }
  }

  onSubmit() {
    if (this.uploadForm.valid) {
      this.isSubmitting = true;

      const files: File[] = [];
      const documentTypes: number[] = [];

      Object.keys(this.uploadedFiles).forEach((key) => {
        files.push(this.uploadedFiles[key]);
        documentTypes.push(Number(key)); 
      });
      console.log("Files to upload:", files);
      console.log("Document types:", documentTypes);
      this.documentService
        .uploadMultipleDocuments(files, documentTypes)
        .subscribe({
          next: (res) => {
            console.log("Upload success", res);
            this.isSubmitting = false;
            this.router.navigate(["/application-review"]);
          },
          error: (err) => {
            console.error("Upload error", err);
            if (err.error && err.error.errors) {
              console.error("Backend errors:", err.error.errors);
            }
            this.isSubmitting = false;
          },
        });
    } else {
      this.uploadForm.markAllAsTouched();
    }
  }
}
