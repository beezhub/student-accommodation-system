import { safeReturnUrl } from './safe-return-url';

describe('safeReturnUrl', () => {
  it('safeReturnUrl_inAppPath_isKept', () => {
    expect(safeReturnUrl('/application-submission')).toBe('/application-submission');
  });

  it('safeReturnUrl_pathWithQuery_isKeptWhole', () => {
    expect(safeReturnUrl('/admin/rooms?building=3')).toBe('/admin/rooms?building=3');
  });

  for (const raw of [null, '', 'admin', '//evil.example', '/\\evil.example', '/x?u=https://e.x']) {
    it(`safeReturnUrl_notAnInAppPath_isRefused: ${raw}`, () => {
      expect(safeReturnUrl(raw)).toBeNull();
    });
  }

  for (const raw of ['/', '/login?returnUrl=%2F', '/email-verification-required']) {
    it(`safeReturnUrl_pageThatWouldLoop_isRefused: ${raw}`, () => {
      expect(safeReturnUrl(raw)).toBeNull();
    });
  }
});
