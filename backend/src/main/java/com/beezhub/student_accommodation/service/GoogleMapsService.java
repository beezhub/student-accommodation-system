package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.GoogleMapsConfig;
import com.beezhub.student_accommodation.model.dto.GeocodingResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsService {

    private final GoogleMapsConfig googleMapsConfig;
    private final RestTemplate restTemplate;

    private static final String GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    /**
     * Geocodes a place ID from the frontend autocomplete
     */
    public GeocodingResult geocodePlaceId(String placeId) {
        if (!googleMapsConfig.isGeocodingEnabled()) {
            log.debug("Geocoding is disabled, skipping place ID geocoding");
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Geocoding is disabled")
                    .build();
        }

        if (placeId == null || placeId.isEmpty()) {
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Place ID is required")
                    .build();
        }

        try {
            String url = String.format("%s?place_id=%s&key=%s",
                    GEOCODING_API_URL,
                    placeId,
                    googleMapsConfig.getApiKey());

            String response = restTemplate.getForObject(url, String.class);
            return parseGeocodingResponse(response);

        } catch (Exception e) {
            log.warn("Failed to geocode place ID {}: {}", placeId, e.getMessage());
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Geocoding API error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Geocodes a manual address entry
     */
    public GeocodingResult geocodeAddress(String address) {
        if (!googleMapsConfig.isGeocodingEnabled()) {
            log.debug("Geocoding is disabled, skipping address geocoding");
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Geocoding is disabled")
                    .build();
        }

        if (address == null || address.isEmpty()) {
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Address is required")
                    .build();
        }

        try {
            String url = String.format("%s?address=%s&key=%s",
                    GEOCODING_API_URL,
                    address.replace(" ", "+"),
                    googleMapsConfig.getApiKey());

            String response = restTemplate.getForObject(url, String.class);
            return parseGeocodingResponse(response);

        } catch (Exception e) {
            log.warn("Failed to geocode address {}: {}", address, e.getMessage());
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Geocoding API error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Parses the Google Geocoding API JSON response
     */
    private GeocodingResult parseGeocodingResponse(String response) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            String status = jsonResponse.get("status").getAsString();

            if (!"OK".equals(status)) {
                String errorMessage = jsonResponse.has("error_message")
                        ? jsonResponse.get("error_message").getAsString()
                        : "Geocoding failed with status: " + status;
                return GeocodingResult.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }

            JsonArray results = jsonResponse.getAsJsonArray("results");
            if (results.size() == 0) {
                return GeocodingResult.builder()
                        .success(false)
                        .errorMessage("No results found")
                        .build();
            }

            JsonObject firstResult = results.get(0).getAsJsonObject();

            // Extract geometry location
            JsonObject geometry = firstResult.getAsJsonObject("geometry");
            JsonObject location = geometry.getAsJsonObject("location");
            double latitude = location.get("lat").getAsDouble();
            double longitude = location.get("lng").getAsDouble();

            // Extract place ID
            String placeId = firstResult.get("place_id").getAsString();

            // Extract formatted address
            String formattedAddress = firstResult.get("formatted_address").getAsString();

            // Extract address components
            Map<String, String> addressComponents = new HashMap<>();
            JsonArray components = firstResult.getAsJsonArray("address_components");
            for (JsonElement component : components) {
                JsonObject componentObj = component.getAsJsonObject();
                JsonArray types = componentObj.getAsJsonArray("types");
                String longName = componentObj.get("long_name").getAsString();
                String shortName = componentObj.get("short_name").getAsString();

                // Store by type
                for (JsonElement type : types) {
                    String typeName = type.getAsString();
                    addressComponents.put(typeName + "_long", longName);
                    addressComponents.put(typeName + "_short", shortName);
                }
            }

            return GeocodingResult.builder()
                    .success(true)
                    .latitude(latitude)
                    .longitude(longitude)
                    .placeId(placeId)
                    .formattedAddress(formattedAddress)
                    .addressComponents(addressComponents)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse geocoding response: {}", e.getMessage());
            return GeocodingResult.builder()
                    .success(false)
                    .errorMessage("Failed to parse geocoding response: " + e.getMessage())
                    .build();
        }
    }
}
