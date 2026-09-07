package com.beezhub.student_accommodation.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.beezhub.student_accommodation.exceptions.ExternalServiceException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * GH-152: address lookup moved from the browser calling Nominatim directly to a backend service.
 *
 * <p>Driven against a local stub rather than the live Google API. That covers everything this class
 * actually decides — when it refuses, what it parses, what it drops, and what it never logs — but
 * not whether Google's real payloads match the shape assumed here. Those records are written from
 * the documented Geocoding API response, and the first run against a real key is what confirms it.
 */
class GoogleGeocodingServiceTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile String responseBody = "{}";
    private volatile int responseStatus = 200;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/geocode", exchange -> {
            requestCount.incrementAndGet();
            // Raw, not decoded: whether a space actually reaches the wire as %20 is the thing
            // worth asserting — an unencoded space would break the request.
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/geocode";
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private GoogleGeocodingService serviceWithKey(String key) {
        return new GoogleGeocodingService(RestClient.builder(), baseUrl, key);
    }

    // ── Not configured ──────────────────────────────────────────────────────────────────────

    /**
     * The default. This is a paid API: a deployment that has not chosen to pay for it should not
     * be quietly making requests, so no key means no calls at all rather than calls that fail.
     */
    @Test
    void withNoApiKey_reportsUnconfiguredAndMakesNoRequest() {
        GoogleGeocodingService service = serviceWithKey("");

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(() -> service.search("1 Main Road"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("not configured");
        assertThat(requestCount.get()).isZero();
    }

    @Test
    void aWhitespaceOnlyKeyIsNotAKey() {
        assertThat(serviceWithKey("   ").isConfigured()).isFalse();
    }

    @Test
    void withAnApiKey_reportsConfigured() {
        assertThat(serviceWithKey("a-real-looking-key").isConfigured()).isTrue();
    }

    // ── Queries not worth spending a request on ─────────────────────────────────────────────

    @Test
    void aQueryTooShortToBeAnAddress_makesNoRequest() {
        GoogleGeocodingService service = serviceWithKey("k");

        assertThat(service.search("ab")).isEmpty();
        assertThat(service.search(" ")).isEmpty();
        assertThat(service.search(null)).isEmpty();
        assertThat(requestCount.get()).isZero();
    }

    // ── Parsing ─────────────────────────────────────────────────────────────────────────────

    @Test
    void aSuccessfulLookup_mapsPlaceIdAddressAndCoordinates() {
        responseBody = """
                {"status":"OK","results":[
                  {"place_id":"ChIJ1","formatted_address":"1 Main Road, Cape Town, South Africa",
                   "geometry":{"location":{"lat":-33.9249,"lng":18.4241}}}
                ]}""";

        List<AddressSuggestion> results = serviceWithKey("k").search("1 Main Road");

        assertThat(results).hasSize(1);
        AddressSuggestion first = results.get(0);
        assertThat(first.placeId()).isEqualTo("ChIJ1");
        assertThat(first.displayName()).isEqualTo("1 Main Road, Cape Town, South Africa");
        assertThat(first.latitude()).isEqualTo(-33.9249);
        assertThat(first.longitude()).isEqualTo(18.4241);
    }

    /** ZERO_RESULTS is an answer — the address matched nothing — not a provider failure. */
    @Test
    void zeroResults_isAnEmptyListRatherThanAnError() {
        responseBody = "{\"status\":\"ZERO_RESULTS\",\"results\":[]}";

        assertThat(serviceWithKey("k").search("nowhere at all")).isEmpty();
    }

    /** A result with no geometry cannot be placed on a map, so it is not offered as a choice. */
    @Test
    void aResultWithoutCoordinates_isDropped() {
        responseBody = """
                {"status":"OK","results":[
                  {"place_id":"no-geo","formatted_address":"Somewhere"},
                  {"place_id":"ok","formatted_address":"1 Main Road",
                   "geometry":{"location":{"lat":1.0,"lng":2.0}}}
                ]}""";

        List<AddressSuggestion> results = serviceWithKey("k").search("1 Main Road");

        assertThat(results).extracting(AddressSuggestion::placeId).containsExactly("ok");
    }

    @Test
    void moreResultsThanTheAutocompleteShows_areCapped() {
        StringBuilder body = new StringBuilder("{\"status\":\"OK\",\"results\":[");
        for (int i = 0; i < 12; i++) {
            body.append(i > 0 ? "," : "")
                    .append("{\"place_id\":\"p").append(i)
                    .append("\",\"formatted_address\":\"a").append(i)
                    .append("\",\"geometry\":{\"location\":{\"lat\":1.0,\"lng\":2.0}}}");
        }
        responseBody = body.append("]}").toString();

        assertThat(serviceWithKey("k").search("main road")).hasSize(5);
    }

    /** Unknown fields are ignored, so Google adding one does not break the lookup. */
    @Test
    void unknownFieldsInTheResponse_areIgnored() {
        responseBody = """
                {"status":"OK","plus_code":{"global_code":"x"},"results":[
                  {"place_id":"ChIJ1","formatted_address":"1 Main Road","types":["street_address"],
                   "geometry":{"location":{"lat":1.0,"lng":2.0},"location_type":"ROOFTOP"}}
                ]}""";

        assertThat(serviceWithKey("k").search("1 Main Road")).hasSize(1);
    }

    // ── Failures ────────────────────────────────────────────────────────────────────────────

    /** REQUEST_DENIED, OVER_QUERY_LIMIT and friends: the provider answered, and said no. */
    @Test
    void aRefusedRequest_isAnExternalServiceFailure() {
        responseBody = "{\"status\":\"REQUEST_DENIED\",\"error_message\":\"The provided API key is invalid.\"}";

        assertThatThrownBy(() -> serviceWithKey("k").search("1 Main Road"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("refused");
    }

    @Test
    void anUnreachableProvider_isAnExternalServiceFailure() {
        GoogleGeocodingService service =
                new GoogleGeocodingService(RestClient.builder(), "http://127.0.0.1:1/geocode", "k");

        assertThatThrownBy(() -> service.search("1 Main Road"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("unreachable");
    }

    @Test
    void aServerErrorFromTheProvider_isAnExternalServiceFailure() {
        responseStatus = 500;
        responseBody = "no";

        assertThatThrownBy(() -> serviceWithKey("k").search("1 Main Road"))
                .isInstanceOf(ExternalServiceException.class);
    }

    // ── What reaches the provider, and what does not ────────────────────────────────────────

    @Test
    void theQueryAndKeyAreSentAsParameters() {
        responseBody = "{\"status\":\"ZERO_RESULTS\",\"results\":[]}";

        serviceWithKey("secret-key").search("1 Main Road");

        assertThat(lastQuery.get()).contains("address=1%20Main%20Road", "key=secret-key");
    }

    /**
     * A trailing space in the box should not be a different query from the same address without
     * one — it would be a second paid request for the same answer.
     */
    @Test
    void theQueryIsTrimmedBeforeItIsSent() {
        responseBody = "{\"status\":\"ZERO_RESULTS\",\"results\":[]}";

        serviceWithKey("k").search("  1 Main Road  ");

        assertThat(lastQuery.get()).contains("address=1%20Main%20Road");
        assertThat(lastQuery.get()).doesNotContain("%20%20");
    }
}
