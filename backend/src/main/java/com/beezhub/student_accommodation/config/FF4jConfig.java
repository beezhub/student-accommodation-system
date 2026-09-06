package com.beezhub.student_accommodation.config;

import lombok.extern.slf4j.Slf4j;
import org.ff4j.FF4j;
import org.ff4j.store.JdbcFeatureStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * Wires FF4j onto the application datasource.
 *
 * <p>The stored row is the source of truth for a flag. This class used to call
 * {@code enable}/{@code disable} from an application property on every context refresh, so a
 * platform admin turning auto-allocation off was silently overwritten at the next boot and batch
 * allocation resumed on its own (GH-142). The property is gone rather than made conditional: a
 * first-boot-only seed would have made the same environment variable meaningful exactly once and
 * silently inert thereafter, which is a subtler version of the same trap.
 *
 * <p>The flag ships disabled, seeded by {@code V2026_02_25_0012__fix_ff4j_schema.sql}, and is
 * turned on through {@code /api/admin/features/auto-allocation/enable}. That matches the platform
 * default for the separate {@code auto_allocation_enabled} system setting.
 */
@Slf4j
@Configuration
public class FF4jConfig {

    public static final String FEATURE_AUTO_ALLOCATION = "auto-allocation";

    private FF4j ff4j;

    /**
     * Touches the database zero times, so it needs no ordering against Flyway. Every read and write
     * happens later, through {@code FF4JFeatureFlagService}, by which point migrations have run.
     */
    @Bean
    public FF4j getFF4j(DataSource dataSource) {
        ff4j = new FF4j();
        ff4j.setFeatureStore(new JdbcFeatureStore(dataSource));

        // Deliberately not autoCreate(true). FF4j 1.9's enable(String)/disable(String) catch
        // FeatureNotFoundException and create the feature when autocreate is on, so
        // POST /api/admin/features/{anything}/enable returned 200 and minted a feature row named
        // from the URL path segment. A platform admin could fill the FF4j tables by typing URLs,
        // and a typo in a real flag name created a lookalike that did nothing rather than failing
        // (GH-203).
        //
        // Nothing relies on autocreate to seed: auto-allocation, the only flag in the codebase, is
        // inserted by V2026_02_19_2101__seed_reference_data.sql. An unknown id now raises
        // FeatureNotFoundException, which FF4JFeatureFlagService turns into a 404.
        ff4j.autoCreate(false);

        return ff4j;
    }

    /**
     * Reports the effective flag once the application is up.
     *
     * <p>This was a {@code @PostConstruct}, which runs while this configuration class is being
     * initialised, before its own {@code @Bean} methods are invoked. The {@code ff4j} field was
     * therefore still null and the null guard made the method a permanent no-op, so the banner it
     * logs had never once been emitted.
     *
     * <p>The catch is required, not defensive: an exception escaping an ApplicationReadyEvent
     * listener fails startup, and contexts without the FF4j tables would take the whole application
     * down over a log line.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logFeatureStatus() {
        try {
            log.info("action=featureFlagStatus feature={} enabled={}",
                    FEATURE_AUTO_ALLOCATION, ff4j.check(FEATURE_AUTO_ALLOCATION));
        } catch (RuntimeException e) {
            log.warn("action=featureFlagStatus feature={} outcome=unavailable error={}",
                    FEATURE_AUTO_ALLOCATION, e.getMessage());
        }
    }
}
