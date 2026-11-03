package com.beezhub.student_accommodation.service.geocoding;

/**
 * One candidate address, as offered to an administrator typing into the building form.
 *
 * <p>Carries no coordinates: Autocomplete does not return them. They come from
 * {@link GeocodingService#resolve} once one suggestion is picked, which is one paid lookup instead
 * of one for every suggestion shown.
 *
 * @param placeId          the provider's stable id for this place
 * @param displayName      the address as the provider formats it, for display and for storing
 */
public record AddressSuggestion(String placeId, String displayName) {
}
