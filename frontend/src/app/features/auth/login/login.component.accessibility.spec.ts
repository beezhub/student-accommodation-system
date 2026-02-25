/**
 * Automated accessibility audit for LoginComponent.
 *
 * Uses @axe-core/angular to run a real axe-core analysis against the rendered
 * component DOM, enforcing WCAG 2.1 AA compliance.
 *
 * Run with: npm test
 */

import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

// @axe-core/angular exposes a Promise-based API compatible with Jasmine/Karma
import { axe, toHaveNoViolations } from '@axe-core/angular';
import { Results } from 'axe-core';

describe('LoginComponent – Accessibility Audit (axe-core)', () => {
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule, ReactiveFormsModule, RouterTestingModule],
      providers: [{ provide: AuthService, useValue: authServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('login form should have no axe accessibility violations', async () => {
    const results: Results = await axe(fixture.nativeElement);

    // Collect violations for a readable error message
    const violations = results.violations;
    const violationMessages = violations
      .map(
        (v) =>
          `[${v.impact}] ${v.id}: ${v.description}\n` +
          v.nodes.map((n) => `  Element: ${n.html}`).join('\n')
      )
      .join('\n');

    expect(violations.length).withContext(`axe violations found:\n${violationMessages}`).toBe(0);
  });
});
