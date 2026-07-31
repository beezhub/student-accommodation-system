## ADDED Requirements

### Requirement: SSE endpoint for offer lifecycle events
The system SHALL provide a `GET /api/sse/offers` endpoint that opens a Server-Sent Events stream for the authenticated student. The stream MUST emit events when the student's offer status changes (new offer, accepted, declined, expired).

#### Scenario: Student connects to SSE stream
- **WHEN** an authenticated student opens a connection to `GET /api/sse/offers`
- **THEN** the server returns `200 OK` with `Content-Type: text/event-stream` and holds the connection open

#### Scenario: Offer status change emits event
- **WHEN** the student has an active SSE connection and their offer status changes from `OFFERED` to `EXPIRED`
- **THEN** the SSE stream emits an event with `type: offer-update` and data containing the new allocation status and expiry date

#### Scenario: New offer emits event
- **WHEN** the student has an active SSE connection and receives a new room offer
- **THEN** the SSE stream emits an event with `type: new-offer` and data containing the allocation details

### Requirement: SSE connection handles authentication
The SSE endpoint MUST authenticate the student. Since `EventSource` does not support custom headers, the JWT token MUST be accepted as a query parameter: `GET /api/sse/offers?token={jwt}`.

#### Scenario: Valid token in query parameter
- **WHEN** a request is made to `GET /api/sse/offers?token={valid_jwt}`
- **THEN** the connection is established and events are scoped to the authenticated student

#### Scenario: Missing or invalid token
- **WHEN** a request is made to `GET /api/sse/offers` without a token or with an expired token
- **THEN** the system returns `401 Unauthorized`

### Requirement: SSE connection timeout and keepalive
Each SSE connection MUST have a server-side timeout of 5 minutes. The server MUST send a keepalive comment (`: keepalive\n\n`) every 30 seconds to prevent proxies from closing the connection. After timeout, the connection is closed and the client must reconnect.

#### Scenario: Keepalive sent during idle connection
- **WHEN** 30 seconds elapse with no events on an active SSE connection
- **THEN** the server sends a comment line `: keepalive` to maintain the connection

#### Scenario: Connection times out after 5 minutes
- **WHEN** 5 minutes elapse on an SSE connection
- **THEN** the server closes the connection and the client's `EventSource` triggers a reconnection

### Requirement: Frontend reconnects with exponential backoff
The frontend SSE client SHALL implement automatic reconnection with exponential backoff: 1s, 2s, 4s, 8s, 16s, 30s (max). The backoff MUST reset to 1s after a successful connection that lasts more than 30 seconds.

#### Scenario: Connection drops and reconnects
- **WHEN** the SSE connection is lost
- **THEN** the client attempts reconnection after 1 second, then 2 seconds if it fails again, doubling up to a 30-second maximum

#### Scenario: Backoff resets after stable connection
- **WHEN** a reconnection succeeds and the connection remains open for more than 30 seconds
- **THEN** the backoff delay resets to 1 second for future reconnection attempts

### Requirement: OfferExpiryComponent uses SSE instead of polling
The `OfferExpiryComponent` SHALL subscribe to the SSE stream for offer updates instead of using a 60-second polling interval. If the SSE connection fails to establish, the component SHALL fall back to the existing 60-second polling mechanism.

#### Scenario: SSE stream provides real-time updates
- **WHEN** the `OfferExpiryComponent` is mounted and SSE is available
- **THEN** the component updates its display immediately when an `offer-update` event arrives, without waiting for a polling cycle

#### Scenario: SSE unavailable falls back to polling
- **WHEN** the `OfferExpiryComponent` is mounted and SSE connection fails after 3 retry attempts
- **THEN** the component activates the 60-second polling fallback and logs a warning
