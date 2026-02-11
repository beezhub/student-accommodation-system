package com.beezhub.student_accommodation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GoogleMapsConfig {

    @Value("${google.maps.api-key:}")
    private String apiKey;

    @Value("${google.maps.frontend.api-key:}")
    private String frontendApiKey;

    @Value("${google.maps.geocoding.enabled:true}")
    private boolean geocodingEnabled;

    public String getApiKey() {
        return apiKey;
    }

    public String getFrontendApiKey() {
        return frontendApiKey;
    }

    public boolean isGeocodingEnabled() {
        return geocodingEnabled && apiKey != null && !apiKey.isEmpty();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
