package com.beezhub.student_accommodation.service.geocoding;

/**
 * One candidate address, as offered to an administrator typing into the building form.
 *
 * @param placeId          the provider's stable id for this place
 * @param displayName      the address as the provider formats it, for display and for storing
 * @param latitude         decimal degrees
 * @param longitude        decimal degrees
 */
public record AddressSuggestion(String placeId, String displayName, double latitude,
                                double longitude) {
}
