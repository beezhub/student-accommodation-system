package com.beezhub.student_accommodation.exceptions;

/**
 * A service this application depends on failed: mail, report generation, or similar.
 *
 * <p>Distinct from the domain exceptions because it is nobody's fault but ours. These were bare
 * {@code RuntimeException}s, and GH-149 had to treat that type as carrying a client-safe message,
 * so an SMTP outage returned 400 and stayed invisible to 5xx alerting (GH-205). Mapped to 503: the
 * caller did nothing wrong and the request may well succeed later.
 *
 * <p>The message is for the log, not the client. The handler returns a fixed sentence and a
 * reference id, because these messages carry hostnames, keys and driver text.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
