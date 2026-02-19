package com.beezhub.student_accommodation.config.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SmtpHealthIndicatorTest {

    @Test
    void health_connectionRefused_statusDown() {
        // Use a port that is unlikely to be open
        SmtpHealthIndicator indicator = new SmtpHealthIndicator(
                mock(JavaMailSender.class),
                "localhost",
                54321);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }
}
