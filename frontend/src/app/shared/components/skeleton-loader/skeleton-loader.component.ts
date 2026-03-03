import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './skeleton-loader.component.html',
  styleUrls: ['./skeleton-loader.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SkeletonLoaderComponent {
  @Input() type: 'card' | 'table-row' | 'text-block' | 'avatar' = 'text-block';
  @Input() count: number = 1;

  get iterableCount(): number[] {
    return Array(this.count).fill(0);
  }
}
