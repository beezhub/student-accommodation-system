import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { BuildingResponse } from '../../../../../core/services/building.service';
import { IntakeOption } from '../../../../../core/models/apply-options.model';
import { describeIntake } from '../../../../../core/utils/apply-wording';
import { CompanyCheck, problemsOf } from '../company-checks';

/**
 * One company on the application form (GH-495): its buildings, what stops it, and the intake.
 * The intake is asked for only where more than one is open; one open intake is chosen for them.
 */
@Component({
  selector: 'app-company-check-panel',
  templateUrl: './company-check-panel.component.html',
  styleUrls: ['./company-check-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
})
export class CompanyCheckPanelComponent {
  @Input({ required: true }) check!: CompanyCheck;
  @Input() buildings: BuildingResponse[] = [];
  @Output() intakeChosen = new EventEmitter<number>();
  @Output() removeCompany = new EventEmitter<void>();

  readonly describeIntake = describeIntake;

  get problems(): string[] {
    return problemsOf(this.check);
  }

  get openIntakes(): IntakeOption[] {
    return this.check.options?.openIntakes ?? [];
  }

  get buildingNames(): string {
    return this.check.buildingIds
      .map((id) => this.buildings.find((b) => b.id === id)?.name)
      .filter((name): name is string => !!name)
      .join(', ');
  }

  get missesDocuments(): boolean {
    return (this.check.options?.missingDocuments.length ?? 0) > 0;
  }
}
