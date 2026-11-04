import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ErrorDisplayComponent, ErrorType } from './error-display.component';

describe('ErrorDisplayComponent', () => {
  let fixture: ComponentFixture<ErrorDisplayComponent>;
  let component: ErrorDisplayComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideNoopAnimations()] });
    fixture = TestBed.createComponent(ErrorDisplayComponent);
    component = fixture.componentInstance;
  });

  it('displayText_nothingGiven_usesTheTextForTheType', () => {
    component.type = 'network';

    expect(component.displayTitle).toBe('Connection Error');
    expect(component.displayMessage).toContain('Unable to connect');
  });

  it('displayText_givenTitleAndMessage_winOverTheDefaults', () => {
    component.title = 'Missing documents';
    component.message = 'Upload your ID copy.';

    expect(component.displayTitle).toBe('Missing documents');
    expect(component.displayMessage).toBe('Upload your ID copy.');
  });

  it('errorConfig_unknownType_fallsBackToGeneral', () => {
    component.type = 'surprise' as ErrorType;

    expect(component.errorConfig.title).toBe('Something went wrong');
  });

  (['network', 'server', 'general'] as ErrorType[]).forEach((type) => {
    it(`showRetryButton_${type}_isShown`, () => {
      component.type = type;

      expect(component.showRetryButton).toBeTrue();
    });
  });

  it('showRetryButton_validationError_isHidden', () => {
    component.type = 'validation';

    expect(component.showRetryButton).toBeFalse();
  });

  it('showRetryButton_retryTurnedOff_isHidden', () => {
    component.type = 'server';
    component.showRetry = false;

    expect(component.showRetryButton).toBeFalse();
  });

  it('onRetry_notRetrying_emits', () => {
    const retry = spyOn(component.retry, 'emit');

    component.onRetry();

    expect(retry).toHaveBeenCalled();
  });

  it('onRetry_alreadyRetrying_doesNotEmitAgain', () => {
    const retry = spyOn(component.retry, 'emit');
    component.isRetrying = true;

    component.onRetry();

    expect(retry).not.toHaveBeenCalled();
  });

  it('actions_eachOne_emitsItsOwnEvent', () => {
    const goBack = spyOn(component.goBack, 'emit');
    const dismiss = spyOn(component.dismiss, 'emit');
    const custom = spyOn(component.customActionClick, 'emit');

    component.onGoBack();
    component.onDismiss();
    component.onCustomAction();

    expect(goBack).toHaveBeenCalled();
    expect(dismiss).toHaveBeenCalled();
    expect(custom).toHaveBeenCalled();
  });

  it('template_inlineMode_showsTheMessageAndDetails', () => {
    component.mode = 'inline';
    component.message = 'Missing documents';
    component.details = ['ID copy', 'Proof of registration'];

    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Missing documents');
    expect(text).toContain('Proof of registration');
  });
});
