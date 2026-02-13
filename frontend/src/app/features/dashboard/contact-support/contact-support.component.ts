import { Component } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-contact-support',
  standalone: true,
  imports: [
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  templateUrl: './contact-support.component.html',
  styleUrls: ['./contact-support.component.css'],
})
export class ContactSupportComponent {
  name = '';
  email = '';
  message = '';
  submitted = false;

  constructor(private snackBar: MatSnackBar) {}

  submitForm() {
    this.submitted = true;
    this.snackBar.open('Your message has been sent. Support will contact you soon.', 'Close', {
      duration: 4000,
      panelClass: 'snackbar-success',
    });
    this.name = '';
    this.email = '';
    this.message = '';
  }
}
