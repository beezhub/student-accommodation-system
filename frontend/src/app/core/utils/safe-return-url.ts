/** Pages a returnUrl must never point at, because going back there would loop. */
const LOOPING_PATHS = ['/', '/login', '/email-verification-required'];

/**
 * Deny-by-default check on a returnUrl query parameter: an in-app path, or null.
 *
 * <p>Angular routing cannot navigate off-origin, so this is defence in depth rather than the
 * thing standing between a user and an open redirect. It also stops a redirect loop back to the
 * page that is reading it, which is the failure a tester would actually hit.
 */
export function safeReturnUrl(raw: string | null): string | null {
  if (!raw || !raw.startsWith('/')) return null;
  if (raw.startsWith('//') || raw.startsWith('/\\')) return null;
  if (raw.includes('://')) return null;
  const path = raw.split(/[?#]/)[0];
  if (LOOPING_PATHS.includes(path)) return null;
  return raw;
}
