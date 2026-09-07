import { environment } from '../../../environments/environment';

/**
 * Whether a request is going to our own API.
 *
 * <p>Every interceptor in this app was written as a deny-list: skip a handful of known public
 * paths, treat everything else as ours. That is only safe while every request goes to our backend,
 * and one did not — the address autocomplete on the building form called
 * {@code nominatim.openstreetmap.org} through the same HttpClient, so it passed through the same
 * chain.
 *
 * <p>That particular call is gone: geocoding moved behind {@code /api/geocoding/search} in GH-152,
 * so the browser no longer talks to a third party. The allow-list stays, because it is the correct
 * posture rather than a fix for one URL — the next third-party call added through this HttpClient
 * should be untouched by default, not by remembering to add it to a deny-list.
 *
 * <p>The consequence was that `authInterceptor` attached the caller's `Authorization: Bearer`
 * header to it. The caller is an administrator, so that is a four-hour admin token, and the request
 * really is transmitted: the host answers the preflight with
 * `access-control-allow-headers: authorization` and `access-control-allow-origin: *`, and the
 * content-security-policy already permits the origin.
 *
 * <p>Two further interceptors misread the same request. `tokenExpiryInterceptor` would sign the
 * user out because a third-party URL is not in its public list, and `errorInterceptor` retried it
 * and reported its failures in app-branded snackbars.
 *
 * <p>An allow-list is the fix: a request is ours only if it targets our API base. Anything else
 * passes through untouched.
 */
export function isOurApiRequest(url: string): boolean {
  const base = environment.apiUrl;

  // Relative in production ('/api'), absolute in development ('http://localhost:8080/api').
  if (url.startsWith(base)) {
    return true;
  }

  // A relative URL is same-origin by definition, so it can only be ours.
  return !/^[a-z][a-z0-9+.-]*:\/\//i.test(url) && !url.startsWith('//');
}
