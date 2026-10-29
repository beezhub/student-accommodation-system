package com.beezhub.student_accommodation.service.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.beezhub.student_accommodation.exceptions.ExternalServiceException;
import com.beezhub.student_accommodation.exceptions.ServiceNotConfiguredException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Against a local stub of Places API (New), so the request shape and the parsing are both
 * exercised without a key or a network.
 */
class GoogleGeocodingServiceTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile String lastMethod;
    private volatile String lastPath;
    private volatile String lastQuery;
    private volatile String lastBody;
    private volatile String lastApiKey;
    private volatile String lastFieldMask;
    private volatile String responseBody = "{}";
    private volatile int responseStatus = 200;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1", exchange -> {
            requestCount.incrementAndGet();
            lastMethod = exchange.getRequestMethod();
            lastPath = exchange.getRequestURI().getRawPath();
            lastQuery = exchange.getRequestURI().getRawQuery();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastApiKey = exchange.getRequestHeaders().getFirst("X-Goog-Api-Key");
            lastFieldMask = exchange.getRequestHeaders().getFirst("X-Goog-FieldMask");
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private GoogleGeocodingService serviceWithKey(String key) {
        return new GoogleGeocodingService(RestClient.builder(), baseUrl, key, "");
    }

    // ── Not configured ──────────────────────────────────────────────────────────────────────

    /**
     * The default. This is a paid API: a deployment that has not chosen to pay for it should not
     * be quietly making requests, so no key means no calls at all rather than calls that fail.
     */
    @Test
    void search_noApiKey_reportsUnconfiguredAndMakesNoRequest() {
        GoogleGeocodingService service = serviceWithKey("");

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(() -> service.search("1 Main Road", null))
                .isInstanceOf(ServiceNotConfiguredException.class)
                .hasMessageContaining("not configured");
        assertThatThrownBy(() -> service.resolve("ChIJ1", null))
                .isInstanceOf(ServiceNotConfiguredException.class);
        assertThat(requestCount.get()).isZero();
    }

    @Test
    void isConfigured_whitespaceOnlyKey_isFalse() {
        assertThat(serviceWithKey("   ").isConfigured()).isFalse();
    }

    @Test
    void isConfigured_withAnApiKey_isTrue() {
        assertThat(serviceWithKey("a-real-looking-key").isConfigured()).isTrue();
    }

    // ── Search ──────────────────────────────────────────────────────────────────────────────

    @Test
    void search_queryTooShortToBeAnAddress_makesNoRequest() {
        GoogleGeocodingService service = serviceWithKey("k");

        assertThat(service.search("ab", null)).isEmpty();
        assertThat(service.search(" ", null)).isEmpty();
        assertThat(service.search(null, null)).isEmpty();
        assertThat(requestCount.get()).isZero();
    }

    /** The reason for Places over Geocoding: a half-typed address still gets suggestions. */
    @Test
    void search_placePredictions_mapToSuggestions() {
        responseBody = """
                {"suggestions":[
                  {"placePrediction":{"placeId":"EiQzMiBU","text":{"text":"32 Tsutsube Street, Soweto, South Africa"}}},
                  {"placePrediction":{"placeId":"ChIJ2","text":{"text":"Tsutsube Street, Soweto, South Africa"}}}
                ]}""";

        List<AddressSuggestion> results = serviceWithKey("k").search("32 Tsutsube", null);

        assertThat(results).containsExactly(
                new AddressSuggestion("EiQzMiBU", "32 Tsutsube Street, Soweto, South Africa"),
                new AddressSuggestion("ChIJ2", "Tsutsube Street, Soweto, South Africa"));
    }

    /** No match is an empty object from Places, not an error. */
    @Test
    void search_noSuggestions_isAnEmptyList() {
        responseBody = "{}";

        assertThat(serviceWithKey("k").search("nowhere at all", null)).isEmpty();
    }

    /** A query prediction is a search term, not a place, so there is nothing to put on a map. */
    @Test
    void search_queryPredictionsAndIncompleteEntries_areDropped() {
        responseBody = """
                {"suggestions":[
                  {"queryPrediction":{"text":{"text":"pizza near Soweto"}}},
                  {"placePrediction":{"placeId":"no-text"}},
                  {"placePrediction":{"placeId":"ok","text":{"text":"1 Main Road"}}}
                ]}""";

        List<AddressSuggestion> results = serviceWithKey("k").search("1 Main Road", null);

        assertThat(results).extracting(AddressSuggestion::placeId).containsExactly("ok");
    }

    @Test
    void search_moreSuggestionsThanTheAutocompleteShows_areCapped() {
        StringBuilder body = new StringBuilder("{\"suggestions\":[");
        for (int i = 0; i < 12; i++) {
            body.append(i > 0 ? "," : "")
                    .append("{\"placePrediction\":{\"placeId\":\"p").append(i)
                    .append("\",\"text\":{\"text\":\"a").append(i).append("\"}}}");
        }
        responseBody = body.append("]}").toString();

        assertThat(serviceWithKey("k").search("main road", null)).hasSize(5);
    }

    @Test
    void search_unknownFieldsInTheResponse_areIgnored() {
        responseBody = """
                {"suggestions":[{"placePrediction":{"place":"places/ChIJ1","placeId":"ChIJ1",
                  "text":{"text":"1 Main Road","matches":[{"endOffset":4}]},"types":["street_address"]}}]}""";

        assertThat(serviceWithKey("k").search("1 Main Road", null)).hasSize(1);
    }

    @Test
    void search_sendsTrimmedInputSessionAndKeyAsPlacesExpects() {
        serviceWithKey("secret-key").search("  1 Main Road  ", "session-1");

        assertThat(lastMethod).isEqualTo("POST");
        assertThat(lastPath).isEqualTo("/v1/places:autocomplete");
        assertThat(lastApiKey).isEqualTo("secret-key");
        assertThat(lastFieldMask).contains("suggestions.placePrediction.placeId");
        assertThat(lastBody).contains("\"input\":\"1 Main Road\"", "\"sessionToken\":\"session-1\"");
        // The key goes in a header, never the URL, where proxies and access logs would keep it.
        assertThat(lastQuery).isNull();
    }

    @Test
    void search_noSessionOrRegion_leavesThemOutOfTheRequest() {
        serviceWithKey("k").search("1 Main Road", null);

        assertThat(lastBody).doesNotContain("sessionToken", "includedRegionCodes");
    }

    @Test
    void search_regionCodeConfigured_limitsSuggestionsToThatCountry() {
        new GoogleGeocodingService(RestClient.builder(), baseUrl, "k", " ZA ").search("1 Main Road", null);

        assertThat(lastBody).contains("\"includedRegionCodes\":[\"za\"]");
    }

    // ── Resolve ─────────────────────────────────────────────────────────────────────────────

    @Test
    void resolve_placeDetails_mapToCoordinates() {
        responseBody = """
                {"id":"EiQzMiBU","location":{"latitude":-26.2485,"longitude":27.8540}}""";

        ResolvedPlace place = serviceWithKey("k").resolve("EiQzMiBU", "session-1");

        assertThat(place).isEqualTo(new ResolvedPlace("EiQzMiBU", -26.2485, 27.8540));
        assertThat(lastMethod).isEqualTo("GET");
        assertThat(lastPath).isEqualTo("/v1/places/EiQzMiBU");
        assertThat(lastQuery).isEqualTo("sessionToken=session-1");
        assertThat(lastFieldMask).isEqualTo("id,location");
    }

    @Test
    void resolve_noSession_sendsNoQuery() {
        responseBody = "{\"id\":\"ChIJ1\",\"location\":{\"latitude\":1.0,\"longitude\":2.0}}";

        serviceWithKey("k").resolve("ChIJ1", null);

        assertThat(lastQuery).isNull();
    }

    /** A place that cannot be put on a map is no use to the form. */
    @Test
    void resolve_noLocation_isAnExternalServiceFailure() {
        responseBody = "{\"id\":\"ChIJ1\"}";

        assertThatThrownBy(() -> serviceWithKey("k").resolve("ChIJ1", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("no location");
    }

    // ── Failures ────────────────────────────────────────────────────────────────────────────

    /** PERMISSION_DENIED is what a key without Places API (New) enabled gets back. */
    @Test
    void search_refusedRequest_isAnExternalServiceFailure() {
        responseStatus = 403;
        responseBody = """
                {"error":{"code":403,"message":"Places API (New) has not been used","status":"PERMISSION_DENIED"}}""";

        assertThatThrownBy(() -> serviceWithKey("k").search("1 Main Road", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("refused");
    }

    /**
     * The handler logs the exception with its causes, and a chained HTTP exception's message is the
     * whole response body. Only the status and reason belong in the log.
     */
    @Test
    void search_refusedRequest_keepsTheResponseBodyOutOfTheException() {
        responseStatus = 403;
        responseBody = """
                {"error":{"code":403,"message":"echo of 32 Tsutsube","status":"PERMISSION_DENIED",
                  "details":[{"reason":"API_KEY_SERVICE_BLOCKED"}]}}""";

        assertThatThrownBy(() -> serviceWithKey("k").search("32 Tsutsube", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasNoCause()
                .message().doesNotContain("Tsutsube");
    }

    @Test
    void search_serverErrorWithAnUnreadableBody_isAnExternalServiceFailure() {
        responseStatus = 500;
        responseBody = "no";

        assertThatThrownBy(() -> serviceWithKey("k").search("1 Main Road", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("refused");
    }

    @Test
    void search_unreachableProvider_isAnExternalServiceFailure() {
        GoogleGeocodingService service =
                new GoogleGeocodingService(RestClient.builder(), "http://127.0.0.1:1/v1", "k", "");

        assertThatThrownBy(() -> service.search("1 Main Road", null))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("unreachable");
    }
}
