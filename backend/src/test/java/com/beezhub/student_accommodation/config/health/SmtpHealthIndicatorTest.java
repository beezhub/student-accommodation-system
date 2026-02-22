package com.beezhub.student_accommodation.config.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.ServerSocket;

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

    @Test
    void health_smtpAvailable_statusUp() throws Exception {
        // Open a real ServerSocket on an ephemeral port to simulate an available SMTP server
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            SmtpHealthIndicator indicator = new SmtpHealthIndicator(
                    mock(JavaMailSender.class),
                    "localhost",
                    port);

            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("status");
            assertThat(health.getDetails().get("status")).isEqualTo("Connected");
        }
    }
}
