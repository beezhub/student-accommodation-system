package com.beezhub.student_accommodation.service.geocoding;

import com.beezhub.student_accommodation.exceptions.ExternalServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Geocoding through the Google Maps Geocoding API (GH-152).
 *
 * <p>Ships inert. With no {@code GOOGLE_MAPS_API_KEY} the service reports itself unconfigured and
 * every call is refused with a 503 — rather than starting up broken, or worse, appearing to work.
 * That is deliberate: this is a paid API, and a deployment that has not chosen to pay for it should
 * not be silently making requests.
 */
@Service
@Slf4j
public class GoogleGeocodingService implements GeocodingService {

    /** Google returns this when the query matched nothing. It is a result, not a failure. */
    private static final String ZERO_RESULTS = "ZERO_RESULTS";
    private static final String OK = "OK";

    /** Shortest query worth spending a request on. */
    private static final int MIN_QUERY_LENGTH = 3;

    /** Cap on suggestions returned, matching what the address autocomplete shows. */
    private static final int MAX_SUGGESTIONS = 5;

    private final RestClient restClient;
    private final String apiKey;

    public GoogleGeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${app.geocoding.google.base-url:https://maps.googleapis.com/maps/api/geocode/json}")
            String baseUrl,
            @Value("${app.geocoding.google.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        if (!isConfigured()) {
            log.info("action=geocodingDisabled reason=noApiKey "
                    + "hint=set GOOGLE_MAPS_API_KEY to enable address lookup");
        }
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public List<AddressSuggestion> search(String query) {
        if (!isConfigured()) {
            // A configuration gap, not an outage — but from the caller's side the service is
            // equally unavailable, and the log line above says which it is.
            throw new ExternalServiceException("Geocoding is not configured");
        }
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        GoogleGeocodeResponse response;
        try {
            response = restClient.get()
                    .uri(uri -> uri.queryParam("address", query.trim())
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(GoogleGeocodeResponse.class);
        } catch (RestClientException e) {
            // The query is not logged: it is an address someone is typing.
            throw new ExternalServiceException("Geocoding provider is unreachable", e);
        }

        if (response == null) {
            throw new ExternalServiceException("Geocoding provider returned no body");
        }
        if (ZERO_RESULTS.equals(response.status())) {
            return List.of();
        }
        if (!OK.equals(response.status())) {
            // status carries REQUEST_DENIED, OVER_QUERY_LIMIT, INVALID_REQUEST and the like. The
            // status is logged because it is the only thing that says which; errorMessage is not,
            // because Google echoes the request into it.
            log.error("action=geocodingFailed status={}", response.status());
            throw new ExternalServiceException("Geocoding provider refused the request");
        }

        return response.results() == null ? List.of()
                : response.results().stream()
                        .filter(GoogleGeocodingService::hasCoordinates)
                        .limit(MAX_SUGGESTIONS)
                        .map(result -> new AddressSuggestion(
                                result.placeId(),
                                result.formattedAddress(),
                                result.geometry().location().lat(),
                                result.geometry().location().lng()))
                        .toList();
    }

    private static boolean hasCoordinates(GoogleResult result) {
        return result != null && result.geometry() != null && result.geometry().location() != null;
    }

    // ── The slice of Google's response this uses ────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleGeocodeResponse(String status, List<GoogleResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleResult(@JsonProperty("place_id") String placeId,
                        @JsonProperty("formatted_address") String formattedAddress,
                        GoogleGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleGeometry(GoogleLocation location) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleLocation(double lat, double lng) {
    }
}
