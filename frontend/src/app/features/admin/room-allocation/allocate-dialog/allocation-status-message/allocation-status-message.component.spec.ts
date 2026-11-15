import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AllocationStatusMessageComponent } from './allocation-status-message.component';

@Component({
  template: `<app-allocation-status-message [ok]="ok"
    >Room is available</app-allocation-status-message
  >`,
  imports: [AllocationStatusMessageComponent],
})
class HostComponent {
  ok = true;
}

describe('AllocationStatusMessageComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  function build(ok: boolean): HTMLElement {
    TestBed.configureTestingModule({ imports: [HostComponent] });
    fixture = TestBed.createComponent(HostComponent);
    fixture.componentInstance.ok = ok;
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.status-message') as HTMLElement;
  }

  it('ok_true_showsTheGreenTickAndTheMessage', () => {
    const message = build(true);

    expect(message.classList).toContain('success');
    expect(message.classList).not.toContain('error');
    expect(message.querySelector('mat-icon')?.textContent?.trim()).toBe('check_circle');
    expect(message.querySelector('span')?.textContent?.trim()).toBe('Room is available');
  });

  it('ok_false_showsTheRedError', () => {
    const message = build(false);

    expect(message.classList).toContain('error');
    expect(message.classList).not.toContain('success');
    expect(message.querySelector('mat-icon')?.textContent?.trim()).toBe('error');
  });
});
