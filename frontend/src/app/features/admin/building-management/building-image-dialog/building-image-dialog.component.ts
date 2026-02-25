import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import {
  BuildingService,
  BuildingImageResponse,
  BuildingResponse,
} from '../../../../core/services/building.service';

@Component({
  selector: 'app-building-image-dialog',
  templateUrl: './building-image-dialog.component.html',
  styleUrls: ['./building-image-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatIconModule,
    MatTooltipModule,
  ],
})
export class BuildingImageDialogComponent implements OnInit {
  images: BuildingImageResponse[] = [];
  isLoading = false;
  isUploading = false;
  selectedFiles: File[] = [];

  constructor(
    private buildingService: BuildingService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<BuildingImageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { building: BuildingResponse }
  ) {}

  ngOnInit(): void {
    this.loadImages();
  }

  loadImages(): void {
    this.isLoading = true;
    this.buildingService.getBuildingImages(this.data.building.id).subscribe({
      next: (images) => {
        this.images = images;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading images:', error);
        this.snackBar.open('Failed to load images', 'Close', { duration: 3000 });
        this.isLoading = false;
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFiles = Array.from(input.files);
      this.uploadFiles();
    }
  }

  uploadFiles(): void {
    if (this.selectedFiles.length === 0) return;

    this.isUploading = true;
    this.buildingService.uploadBuildingImages(this.data.building.id, this.selectedFiles).subscribe({
      next: (newImages) => {
        this.snackBar.open(`${newImages.length} image(s) uploaded successfully`, 'Close', {
          duration: 3000,
        });
        this.selectedFiles = [];
        this.loadImages();
        this.isUploading = false;
      },
      error: (error) => {
        console.error('Error uploading images:', error);
        this.snackBar.open(error.error?.message || 'Failed to upload images', 'Close', {
          duration: 5000,
        });
        this.isUploading = false;
      },
    });
  }

  setPrimary(image: BuildingImageResponse): void {
    if (image.isPrimary) return;

    this.buildingService.setPrimaryImage(this.data.building.id, image.id).subscribe({
      next: () => {
        this.snackBar.open('Primary image updated', 'Close', { duration: 2000 });
        this.loadImages();
      },
      error: (error) => {
        console.error('Error setting primary image:', error);
        this.snackBar.open('Failed to set primary image', 'Close', { duration: 3000 });
      },
    });
  }

  deleteImage(image: BuildingImageResponse): void {
    if (!confirm('Are you sure you want to delete this image?')) return;

    this.buildingService.deleteBuildingImage(this.data.building.id, image.id).subscribe({
      next: () => {
        this.snackBar.open('Image deleted successfully', 'Close', { duration: 2000 });
        this.loadImages();
      },
      error: (error) => {
        console.error('Error deleting image:', error);
        this.snackBar.open('Failed to delete image', 'Close', { duration: 3000 });
      },
    });
  }

  onClose(): void {
    this.dialogRef.close(this.images.length > 0);
  }
}
