import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-owner-awaiting-approval',
  templateUrl: './owner-awaiting-approval.component.html',
  styleUrls: ['./owner-awaiting-approval.component.css'],
  standalone: true,
  imports: [RouterModule, MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OwnerAwaitingApprovalComponent {}
