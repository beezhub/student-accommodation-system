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

  it('returning resident: shows the badge', () => {
    expect(build({ returning: true })).toContain('Returning');
  });

  it('both flags: shows both', () => {
    const text = build({ expired: true, returning: true });

    expect(text).toContain('Returning');
    expect(text).toContain('Expired document');
  });

  it('no flags: shows nothing', () => {
    expect(build({ expired: false, returning: false })).toBe('');
  });
});
