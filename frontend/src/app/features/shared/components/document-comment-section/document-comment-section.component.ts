import { Component, Input, OnInit, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { DocumentCommentService } from '../../../../core/services/document-comment.service';
import { DocumentComment } from '../../../../core/models/allocation.models';
import { AuthService } from '../../../../core/services/auth.service';
import { RoleService } from '../../../../core/services/role.service';
import { TimeagoPipe } from '../../../../shared/pipes/timeago.pipe';

@Component({
  selector: 'app-document-comment-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    TimeagoPipe,
  ],
  templateUrl: './document-comment-section.component.html',
  styleUrls: ['./document-comment-section.component.css'],
})
export class DocumentCommentSectionComponent implements OnInit, OnChanges {
  private documentCommentService = inject(DocumentCommentService);
  private authService = inject(AuthService);
  private roleService = inject(RoleService);
  private snackBar = inject(MatSnackBar);

  @Input() documentId!: number;
  @Input() prefillRejectionMessage = false;

  comments: DocumentComment[] = [];
  isLoading = false;
  isSubmitting = false;
  isAdmin = false;

  commentControl = new FormControl('', [Validators.minLength(10), Validators.maxLength(500)]);

  private readonly DEFAULT_REJECTION_MESSAGE =
    'Your document has been rejected. Please review the requirements and submit a new document that meets the specified criteria.';

  ngOnInit(): void {
    this.checkAdminStatus();
    if (this.documentId) {
      this.loadComments();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['documentId'] && !changes['documentId'].firstChange) {
      this.loadComments();
    }
    if (changes['prefillRejectionMessage'] && this.prefillRejectionMessage && this.isAdmin) {
      this.commentControl.setValue(this.DEFAULT_REJECTION_MESSAGE);
    }
  }

  private checkAdminStatus(): void {
    // The roles DocumentController lets add and delete comments. Not RoleService.isAdmin(): that
    // also counts BUILDING_STAFF, which the backend refuses here.
    this.isAdmin =
      this.roleService.isPlatformAdmin() ||
      this.roleService.isPlatformStaff() ||
      this.roleService.isTenantAdmin();
  }

  loadComments(): void {
    if (!this.documentId) {
      return;
    }

    this.isLoading = true;
    this.documentCommentService.getComments(this.documentId).subscribe({
      next: (comments) => {
        this.comments = comments.sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Failed to load comments:', error);
        this.snackBar.open('Failed to load comments', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  submitComment(): void {
    if (this.commentControl.invalid || !this.documentId) {
      return;
    }

    const commentText = this.commentControl.value?.trim();
    if (!commentText) {
      return;
    }

    this.isSubmitting = true;
    this.documentCommentService.addComment(this.documentId, commentText).subscribe({
      next: (newComment) => {
        this.comments.push(newComment);
        this.commentControl.reset();
        this.snackBar.open('Comment added successfully', 'Close', { duration: 3000 });
        this.isSubmitting = false;
      },
      error: (error) => {
        console.error('Failed to add comment:', error);
        const errorMessage = error.error?.message || 'Failed to add comment';
        this.snackBar.open(errorMessage, 'Close', { duration: 3000 });
        this.isSubmitting = false;
      },
    });
  }

  deleteComment(comment: DocumentComment): void {
    this.documentCommentService.deleteComment(comment.id).subscribe({
      next: () => {
        this.comments = this.comments.filter((c) => c.id !== comment.id);
        this.snackBar.open('Comment deleted', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Failed to delete comment:', error);
        this.snackBar.open('Failed to delete comment', 'Close', { duration: 3000 });
      },
    });
  }

  get canSubmit(): boolean {
    return (
      this.commentControl.valid &&
      !!this.commentControl.value?.trim() &&
      this.commentControl.value.trim().length >= 10 &&
      !this.isSubmitting
    );
  }

  get charactersRemaining(): number {
    return 500 - (this.commentControl.value?.length || 0);
  }
}
