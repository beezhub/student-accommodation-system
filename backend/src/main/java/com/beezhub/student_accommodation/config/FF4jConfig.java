package com.beezhub.student_accommodation.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ff4j.FF4j;
import org.ff4j.store.JdbcFeatureStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FF4jConfig {

    public static final String FEATURE_AUTO_ALLOCATION = "auto-allocation";

    @Value("${app.features.auto-allocation.enabled:true}")
    private boolean autoAllocationEnabled;

    private FF4j ff4j;

    @Bean
    public FF4j getFF4j(DataSource dataSource) {
        ff4j = new FF4j();

        // Use JDBC store for persistence
        JdbcFeatureStore featureStore = new JdbcFeatureStore(dataSource);
        ff4j.setFeatureStore(featureStore);

        // Features are managed by Flyway migrations, not auto-created
        ff4j.autoCreate(false);

        // Synchronize feature flag with application property
        try {
            if (autoAllocationEnabled) {
                ff4j.enable(FEATURE_AUTO_ALLOCATION);
                log.info("Auto-allocation feature ENABLED via configuration");
            } else {
                ff4j.disable(FEATURE_AUTO_ALLOCATION);
                log.info("Auto-allocation feature DISABLED via configuration");
            }
        } catch (Exception e) {
            log.error("Failed to configure auto-allocation feature flag: {}", e.getMessage());
        }

        return ff4j;
    }

    @PostConstruct
    public void logFeatureStatus() {
        if (ff4j != null) {
            try {
                log.info("=== Feature Flags Configuration ===");
                log.info("Auto-Allocation: {} (configured: {})",
                        ff4j.check(FEATURE_AUTO_ALLOCATION) ? "ENABLED" : "DISABLED",
                        autoAllocationEnabled);
            } catch (Exception e) {
                log.warn("Unable to check feature flag status at startup: {}", e.getMessage());
            }
        }
    }
}
