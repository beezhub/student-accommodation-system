package com.beezhub.student_accommodation.service.geocoding;

/**
 * Where a picked {@link AddressSuggestion} is on the map.
 *
 * @param placeId          the provider's stable id for this place
 * @param latitude         decimal degrees
 * @param longitude        decimal degrees
 */
public record ResolvedPlace(String placeId, double latitude, double longitude) {
}
