package com.beezhub.student_accommodation.exceptions;

/**
 * One item of a scheduled job failed and its transaction must roll back.
 *
 * <p>Thrown inside the expiry sweeps, whose loops catch it per allocation so one bad row does not
 * abandon the rest. It exists so those sites are not bare {@code RuntimeException}s: nothing about
 * a sweep failure is safe to echo, and a bare runtime type was treated as client-safe (GH-205).
 *
 * <p>Deliberately unmapped in {@code RestResponseExceptionHandler}. No request path throws it; if
 * one ever did, the catch-all's generic 500 is the right answer.
 */
public class ScheduledJobException extends RuntimeException {

    public ScheduledJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
