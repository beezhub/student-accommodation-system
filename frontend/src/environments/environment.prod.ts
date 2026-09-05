/**
 * Production build environment.
 *
 * `apiUrl` is a same-origin relative path, so the app talks to whatever host serves it and the
 * image needs no rebuild per environment. nginx proxies `/api/` to the backend (see nginx.conf).
 * The development file points at http://localhost:8080/api instead, and angular.json swaps this
 * file in for the production configuration.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
