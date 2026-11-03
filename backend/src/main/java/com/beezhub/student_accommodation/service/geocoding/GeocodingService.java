package com.beezhub.student_accommodation.service.geocoding;

import java.util.List;

/**
 * Turns a typed address into candidate places with coordinates (GH-152).
 *
 * <p>An interface because the provider is a deployment choice and the call sites should not know
 * which one is in use — the same shape as {@code FileStorageService} and {@code FeatureFlagService}
 * elsewhere in this codebase.
 *
 * <p>Geocoding was previously done by the browser calling nominatim.openstreetmap.org directly,
 * with {@code BuildingController.getMapsConfig} handing out {@code "dummy-api-key-for-development"}
 * to a frontend that never used it. Moving it here means the key is never client-visible, the
 * provider is configuration rather than a hardcoded URL, and the call can be authorized — each one
 * costs money.
 */
public interface GeocodingService {

    /**
     * Whether the service can actually reach a provider. False when no key is configured.
     *
     * <p>Exposed so the maps-config endpoint can tell the frontend to disable the autocomplete
     * rather than leaving an administrator typing into a field that silently returns nothing.
     */
    boolean isConfigured();

    /**
     * Candidate addresses for a partly typed query, best first, or an empty list when there are
     * none.
     *
     * @param sessionToken groups the requests one person makes while picking an address, through
     *     to the {@link #resolve} that ends it, so the provider bills them as one session. May be
     *     null, which bills each request on its own.
     * @throws com.beezhub.student_accommodation.exceptions.ExternalServiceException if the
     *     provider is unreachable, refuses the request, or is not configured
     */
    List<AddressSuggestion> search(String query, String sessionToken);

    /**
     * The coordinates of a suggestion that was picked.
     *
     * @param sessionToken the same token the {@link #search} calls used, or null
     * @throws com.beezhub.student_accommodation.exceptions.ExternalServiceException if the
     *     provider is unreachable, refuses the request, is not configured, or returns no location
     */
    ResolvedPlace resolve(String placeId, String sessionToken);
}
