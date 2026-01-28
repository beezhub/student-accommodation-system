package com.beezhub.student_accommodation.config;

import org.ff4j.FF4j;
import org.ff4j.store.JdbcFeatureStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FF4jConfig {

    public static final String FEATURE_AUTO_ALLOCATION = "auto-allocation";

    @Bean
    public FF4j getFF4j(DataSource dataSource) {
        FF4j ff4j = new FF4j();

        // Use JDBC store for persistence
        JdbcFeatureStore featureStore = new JdbcFeatureStore(dataSource);
        ff4j.setFeatureStore(featureStore);

        // Features are managed by Flyway migrations, not auto-created
        ff4j.autoCreate(false);

        return ff4j;
    }
}
