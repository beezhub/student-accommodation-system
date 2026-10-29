package com.beezhub.student_accommodation.service.geocoding;

import com.beezhub.student_accommodation.exceptions.ExternalServiceException;
import com.beezhub.student_accommodation.exceptions.ServiceNotConfiguredException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Address lookup through Google Places API (New): Autocomplete for suggestions, Place Details for
 * the coordinates of the one picked (GH-152).
 *
 * <p>This used the Geocoding API, which is built for complete addresses. A half-typed query such as
 * "32 Tsutsube" matched nothing, and an address whose house number Google does not hold came back
 * as the bare street, number dropped and pin in the middle of the road. Autocomplete is built for
 * a box someone is typing into.
 *
 * <p>Ships inert. With no {@code GOOGLE_MAPS_API_KEY} the service reports itself unconfigured and
 * every call is refused with a 503 — rather than starting up broken, or worse, appearing to work.
 * That is deliberate: this is a paid API, and a deployment that has not chosen to pay for it should
 * not be silently making requests.
 */
@Service
@Slf4j
public class GoogleGeocodingService implements GeocodingService {

    private static final String API_KEY_HEADER = "X-Goog-Api-Key";
    private static final String FIELD_MASK_HEADER = "X-Goog-FieldMask";

    /** Only the fields this reads. Places bills by the fields requested. */
    private static final String AUTOCOMPLETE_FIELDS =
            "suggestions.placePrediction.placeId,suggestions.placePrediction.text.text";
    private static final String DETAILS_FIELDS = "id,location";

    /** Shortest query worth spending a request on. */
    private static final int MIN_QUERY_LENGTH = 3;

    /** Cap on suggestions returned, matching what the address autocomplete shows. */
    private static final int MAX_SUGGESTIONS = 5;

    private final RestClient restClient;
    private final String apiKey;
    private final List<String> regionCodes;

    public GoogleGeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${app.geocoding.google.base-url:https://places.googleapis.com/v1}") String baseUrl,
            @Value("${app.geocoding.google.api-key:}") String apiKey,
            @Value("${app.geocoding.google.region-code:}") String regionCode) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.regionCodes = regionCode == null || regionCode.isBlank()
                ? null : List.of(regionCode.trim().toLowerCase());
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
    public List<AddressSuggestion> search(String query, String sessionToken) {
        requireConfigured();
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        AutocompleteRequest request =
                new AutocompleteRequest(query.trim(), blankToNull(sessionToken), regionCodes);
        AutocompleteResponse response = call(() -> restClient.post()
                .uri("/places:autocomplete")
                .header(API_KEY_HEADER, apiKey)
                .header(FIELD_MASK_HEADER, AUTOCOMPLETE_FIELDS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AutocompleteResponse.class));

        // No suggestions comes back as an empty object, not an error.
        if (response == null || response.suggestions() == null) {
            return List.of();
        }
        return response.suggestions().stream()
                .filter(Objects::nonNull)
                .map(Suggestion::placePrediction)
                .filter(GoogleGeocodingService::isUsable)
                .limit(MAX_SUGGESTIONS)
                .map(prediction -> new AddressSuggestion(prediction.placeId(), prediction.text().text()))
                .toList();
    }

    @Override
    public ResolvedPlace resolve(String placeId, String sessionToken) {
        requireConfigured();
        String token = blankToNull(sessionToken);

        PlaceDetails details = call(() -> restClient.get()
                .uri(uri -> {
                    uri.path("/places/{placeId}");
                    // Ends the billing session the autocomplete requests opened.
                    if (token != null) {
                        uri.queryParam("sessionToken", token);
                    }
                    return uri.build(placeId);
                })
                .header(API_KEY_HEADER, apiKey)
                .header(FIELD_MASK_HEADER, DETAILS_FIELDS)
                .retrieve()
                .body(PlaceDetails.class));

        if (details == null || details.location() == null) {
            throw new ExternalServiceException("Geocoding provider returned no location");
        }
        return new ResolvedPlace(placeId, details.location().latitude(), details.location().longitude());
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            // A configuration gap, not an outage, but from the caller's side the service is
            // equally unavailable, and the startup log line says which it is.
            throw new ServiceNotConfiguredException("Geocoding is not configured");
        }
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            // Places refuses with an HTTP error, a status such as PERMISSION_DENIED or
            // RESOURCE_EXHAUSTED, and a reason such as API_KEY_SERVICE_BLOCKED (the key's API
            // restrictions leave Places out). Those two say what to fix, so they are logged. The
            // exception is not chained: its message is the whole response body, which can echo
            // the request, and the request is an address someone is typing.
            PlacesError error = errorOf(e);
            log.error("action=geocodingFailed httpStatus={} status={} reason={}",
                    e.getStatusCode().value(), error.status(), error.reason());
            throw new ExternalServiceException("Geocoding provider refused the request");
        } catch (RestClientException e) {
            throw new ExternalServiceException("Geocoding provider is unreachable", e);
        }
    }

    private static PlacesError errorOf(RestClientResponseException e) {
        try {
            PlacesErrorResponse body = e.getResponseBodyAs(PlacesErrorResponse.class);
            if (body != null && body.error() != null) {
                return body.error();
            }
        } catch (RuntimeException unreadable) {
            // Not JSON, such as a proxy's HTML error page. The HTTP status is still logged.
        }
        return new PlacesError(null, null);
    }

    private static boolean isUsable(PlacePrediction prediction) {
        return prediction != null && prediction.placeId() != null
                && prediction.text() != null && prediction.text().text() != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ── The slice of Places' request and responses this uses ────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AutocompleteRequest(String input, String sessionToken, List<String> includedRegionCodes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AutocompleteResponse(List<Suggestion> suggestions) {
    }

    /** Also carries queryPrediction, a search term rather than a place, which is not wanted here. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Suggestion(PlacePrediction placePrediction) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacePrediction(String placeId, FormattableText text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FormattableText(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlaceDetails(String id, LatLng location) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LatLng(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacesErrorResponse(PlacesError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PlacesError(String status, List<ErrorDetail> details) {

        /** The first detail that names a reason, or null. */
        String reason() {
            return details == null ? null : details.stream()
                    .filter(Objects::nonNull)
                    .map(ErrorDetail::reason)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorDetail(String reason) {
    }
}
