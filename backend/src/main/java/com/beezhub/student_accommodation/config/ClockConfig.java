package com.beezhub.student_accommodation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The one clock for calendar decisions: which academic year is current, and whether an
 * application window is open (GH-494).
 *
 * <p>Pinned to {@code Africa/Johannesburg} rather than taken from the JVM, so "closes 30 June"
 * means midnight on 30 June in South Africa whatever {@code TZ} the container runs with. A window
 * compared in UTC stayed open until 02:00 on 1 July. Inject this bean and call
 * {@code LocalDate.now(clock)} instead of {@code LocalDate.now()} for every new date check, so a
 * test can fix the time.
 */
@Configuration
public class ClockConfig {

    /** The zone every application window and academic year date is read in. */
    public static final ZoneId CALENDAR_ZONE = ZoneId.of("Africa/Johannesburg");

    @Bean
    public Clock clock() {
        return Clock.system(CALENDAR_ZONE);
    }
}
