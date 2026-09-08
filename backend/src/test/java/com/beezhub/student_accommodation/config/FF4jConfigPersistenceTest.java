package com.beezhub.student_accommodation.config;

import jakarta.annotation.PostConstruct;
import org.ff4j.FF4j;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GH-142: the FF4j bean called {@code enable} or {@code disable} from an application property on
 * every context refresh. The store is persistent, so a platform admin turning auto-allocation off
 * was overwritten at the next boot and batch allocation silently resumed.
 *
 * <p>These tests use a real {@code JdbcFeatureStore} over in-memory H2 rather than an
 * {@code InMemoryFeatureStore}, because the defect is specifically that a stored value does not
 * survive. A second {@link FF4jConfig} instance over the same datasource is the restart: that is
 * exactly what a redeploy does, and it is the step no existing test performed.
 */
class FF4jConfigPersistenceTest {

    /** A fresh, empty database per test, kept alive for the duration of the JVM. */
    private static DataSource freshDatabase(String name) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        FF4j schemaBuilder = new FF4j();
        org.ff4j.store.JdbcFeatureStore store = new org.ff4j.store.JdbcFeatureStore(dataSource);
        store.createSchema();
        schemaBuilder.setFeatureStore(store);
        return dataSource;
    }

    /** Stands in for a process restart: a new configuration object over the same database. */
    private static FF4j boot(DataSource dataSource) {
        return new FF4jConfig().getFF4j(dataSource);
    }

    @Test
    void restartAfterAdminDisable_leavesFlagDisabled() {
        DataSource dataSource = freshDatabase("ff4j142disable");

        FF4j firstBoot = boot(dataSource);
        firstBoot.createFeature(FF4jConfig.FEATURE_AUTO_ALLOCATION, true, "seeded enabled");
        assertTrue(firstBoot.check(FF4jConfig.FEATURE_AUTO_ALLOCATION));

        // What a platform admin does through /api/admin/features/auto-allocation/disable.
        firstBoot.disable(FF4jConfig.FEATURE_AUTO_ALLOCATION);

        assertFalse(boot(dataSource).check(FF4jConfig.FEATURE_AUTO_ALLOCATION),
                "an admin turning the flag off must survive a restart");
    }

    @Test
    void restartAfterAdminEnable_leavesFlagEnabled() {
        DataSource dataSource = freshDatabase("ff4j142enable");

        FF4j firstBoot = boot(dataSource);
        firstBoot.createFeature(FF4jConfig.FEATURE_AUTO_ALLOCATION, false, "seeded disabled");
        firstBoot.enable(FF4jConfig.FEATURE_AUTO_ALLOCATION);

        assertTrue(boot(dataSource).check(FF4jConfig.FEATURE_AUTO_ALLOCATION),
                "an admin turning the flag on must survive a restart");
    }

    /**
     * The bean must not read or write the store while it is being built. That is what removes any
     * ordering dependency on Flyway: every access happens later, through the feature-flag service,
     * by which point the migrations have run.
     */
    @Test
    void buildingTheBean_doesNotTouchTheStore() {
        DriverManagerDataSource unreachable = new DriverManagerDataSource(
                "jdbc:h2:mem:ff4j142unreachable;MODE=PostgreSQL", "sa", "");
        unreachable.setDriverClassName("org.h2.Driver");

        // No schema was created here, so any read or write during bean creation would throw.
        assertNotNull(boot(unreachable));
    }

    /**
     * {@code @PostConstruct} ran while the configuration class itself was initialising, before its
     * own {@code @Bean} methods were invoked, so the field was null and the guarded body never ran.
     * The status banner had never been emitted.
     */
    @Test
    void featureStatusIsLoggedAfterStartup_notDuringConstruction() throws Exception {
        Method logFeatureStatus = FF4jConfig.class.getMethod("logFeatureStatus");

        assertNull(logFeatureStatus.getAnnotation(PostConstruct.class),
                "PostConstruct runs before the bean method that assigns the field");
        assertNotNull(AnnotatedElementUtils.findMergedAnnotation(logFeatureStatus, EventListener.class));
    }

    /**
     * An exception escaping an ApplicationReadyEvent listener fails startup, so a context whose
     * database has no FF4j tables must not be brought down by a log line.
     */
    @Test
    void featureStatusLogging_withNoFf4jTables_doesNotThrow() {
        DriverManagerDataSource noTables = new DriverManagerDataSource(
                "jdbc:h2:mem:ff4j142notables;MODE=PostgreSQL", "sa", "");
        noTables.setDriverClassName("org.h2.Driver");

        FF4jConfig config = new FF4jConfig();
        config.getFF4j(noTables);

        config.logFeatureStatus();
    }
}
