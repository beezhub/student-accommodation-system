import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-contact-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contact-support.component.html',
  styleUrls: ['./contact-support.component.css']
})
export class ContactSupportComponent {
  name = '';
  email = '';
  message = '';
  submitted = false;

  submitForm() {
    this.submitted = true;
    // Here you would typically send the form data to your backend
    // For now, just reset the form
    this.name = '';
    this.email = '';
    this.message = '';
  }
}