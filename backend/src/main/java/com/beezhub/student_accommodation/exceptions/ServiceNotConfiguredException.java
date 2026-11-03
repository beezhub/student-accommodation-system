package com.beezhub.student_accommodation.exceptions;

/**
 * An optional integration was called on a deployment that has not configured it, such as
 * geocoding with no API key.
 *
 * <p>Still a 503 to the caller, so it extends {@link ExternalServiceException}. It is its own type
 * because the handler logs it differently: nothing failed, so it is not an ERROR with a stack
 * trace. Logged that way, every keystroke in the address field wrote one.
 */
public class ServiceNotConfiguredException extends ExternalServiceException {

    public ServiceNotConfiguredException(String message) {
        super(message);
    }
}
