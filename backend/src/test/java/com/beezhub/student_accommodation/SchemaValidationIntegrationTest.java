package com.beezhub.student_accommodation;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-148: production boots with {@code hbm2ddl.auto=validate}, but the integration base class
 * re-registered that key as {@code none}, so an entity field added without its Flyway migration
 * passed the whole suite and failed only at real application startup.
 *
 * <p>This class does not itself detect drift, and its name should not be read as promising that.
 * Hibernate validates during {@code LocalContainerEntityManagerFactoryBean.afterPropertiesSet()},
 * so when an entity and its migration disagree every {@link AbstractIntegrationTest} subclass fails
 * at context load with {@code SchemaManagementException} and the test below never runs.
 *
 * <p>Its one job is to go red if someone sets {@code hbm2ddl.auto} back to {@code none} to make a
 * build green. That reverts the guard silently and invisibly, which is how the gap arose.
 *
 * <p>It declares no annotations of its own so it shares the cached integration context rather than
 * starting a seventh one.
 */
class SchemaValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Asserts on what Hibernate was actually built with, not on the Spring {@code Environment}
     * string. Boot's {@code HibernateProperties} removes the setting outright when the resolved
     * value is {@code none}, so the Environment and the session factory can disagree — which is the
     * same {@code spring.jpa.properties.*} versus {@code spring.jpa.hibernate.ddl-auto} precedence
     * trap that let this happen in the first place.
     */
    @Test
    void hbm2ddlAuto_inIntegrationContext_isValidate() {
        assertThat(entityManagerFactory.getProperties())
                .containsEntry("hibernate.hbm2ddl.auto", "validate");
    }
}
