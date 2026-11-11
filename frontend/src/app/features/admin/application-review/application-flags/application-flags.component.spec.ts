import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ApplicationFlagsComponent } from './application-flags.component';

describe('ApplicationFlagsComponent', () => {
  let fixture: ComponentFixture<ApplicationFlagsComponent>;

  function build(inputs: Record<string, boolean | undefined>): string {
    TestBed.configureTestingModule({ imports: [ApplicationFlagsComponent] });
    fixture = TestBed.createComponent(ApplicationFlagsComponent);
    Object.entries(inputs).forEach(([name, value]) => fixture.componentRef.setInput(name, value));
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent?.trim() ?? '';
  }

  it('expired document: shows the badge', () => {
    expect(build({ expired: true })).toContain('Expired document');
  });

  it('no flags: shows nothing', () => {
    expect(build({ expired: false })).toBe('');
  });
});
