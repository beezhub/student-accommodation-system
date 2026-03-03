## ADDED Requirements

### Requirement: Security headers are set on all responses
The Nginx configuration MUST add the following HTTP security headers on every response:
- `X-Frame-Options: SAMEORIGIN`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Content-Security-Policy` with at minimum `default-src 'self'`

#### Scenario: Security headers present on static asset responses
- **WHEN** a browser requests any static asset from the frontend
- **THEN** the response includes `X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`, `Referrer-Policy`, and `Content-Security-Policy` headers

#### Scenario: Security headers present on API proxy responses
- **WHEN** a browser request is proxied to the backend API
- **THEN** the response includes the same security headers

### Requirement: CORS is restricted to known origins
The Nginx configuration MUST NOT use `Access-Control-Allow-Origin: *`. The allowed origin MUST be configurable via an environment variable or template and SHALL default to `http://localhost:4200` for local development. The `Access-Control-Allow-Headers` setting MUST only list the specific headers needed by the application.

#### Scenario: Requests from allowed origin succeed
- **WHEN** a browser sends a cross-origin request from `http://localhost:4200`
- **THEN** the response includes `Access-Control-Allow-Origin: http://localhost:4200`

#### Scenario: Requests from unknown origin are not granted CORS
- **WHEN** a browser sends a cross-origin request from an arbitrary external origin
- **THEN** the response does NOT include an `Access-Control-Allow-Origin` header that would permit the request

### Requirement: API proxy has defined timeouts
The `/api/` location block in Nginx MUST define `proxy_connect_timeout`, `proxy_read_timeout`, and `proxy_send_timeout`. No proxy connection to the backend SHALL be left open indefinitely.

#### Scenario: Proxy timeout values present
- **WHEN** the `nginx.conf` is read
- **THEN** `proxy_connect_timeout`, `proxy_read_timeout`, and `proxy_send_timeout` are defined with non-zero values in the `/api/` location block

#### Scenario: Long-running backend request times out
- **WHEN** the backend does not respond within `proxy_read_timeout`
- **THEN** Nginx closes the connection and returns a 504 Gateway Timeout to the client

### Requirement: HTTP to HTTPS redirect is scaffolded
The Nginx configuration MUST include a commented-out or conditionally-active server block that performs HTTP→HTTPS redirect (301) and configures a TLS listener on port 443. This block MUST be documentationally complete so that enabling it requires only uncommenting and providing a certificate path.

#### Scenario: HTTPS redirect block is present in config
- **WHEN** the `nginx.conf` is read
- **THEN** a server block for port 443 with TLS configuration is present (may be commented out for local dev)
