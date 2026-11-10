package com.beezhub.student_accommodation.exceptions;

/**
 * Thrown when an academic year or intake would have impossible dates: a year that ends before it
 * starts, a window that closes before it opens, or an intake outside its year. The message is
 * written for the administrator editing the calendar.
 */
public class InvalidAcademicCalendarException extends RuntimeException {
    public InvalidAcademicCalendarException(String message) {
        super(message);
    }
}
