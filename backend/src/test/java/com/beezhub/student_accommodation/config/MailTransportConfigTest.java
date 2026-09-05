package com.beezhub.student_accommodation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GH-132: a hand-built {@code JavaMailSenderImpl} set only host, port, username and password, which
 * switched off Boot's mail auto-configuration. {@code spring.mail.properties.mail.smtp.*} never
 * reached the sender, so {@code MAIL_SMTP_AUTH} and {@code MAIL_STARTTLS_ENABLE} did nothing and a
 * real provider would reject the mail. The timeout lines were commented out, so a hung host pinned
 * a notification thread indefinitely.
 */
class MailTransportConfigTest {

    private static String applicationYml() throws IOException {
        return Files.readString(new ClassPathResource("application.yml").getFile().toPath(),
                StandardCharsets.UTF_8);
    }

    @Test
    void theHandBuiltSenderIsGone() {
        // Its existence is what stopped MailSenderAutoConfiguration applying the properties.
        assertThatThrownBy(() -> Class.forName("com.beezhub.student_accommodation.config.MailConfig"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "connectiontimeout: ${MAIL_CONNECT_TIMEOUT_MS:5000}",
            "timeout: ${MAIL_READ_TIMEOUT_MS:5000}",
            "writetimeout: ${MAIL_WRITE_TIMEOUT_MS:5000}"
    })
    void everySmtpTimeoutIsConfigured(String expected) throws IOException {
        // JavaMail waits forever by default; all three have to be set to bound a hung host.
        assertThat(applicationYml()).contains(expected);
    }

    @Test
    void authAndStarttlsRemainEnvironmentDriven() throws IOException {
        String config = applicationYml();

        assertThat(config).contains("auth: ${MAIL_SMTP_AUTH:false}");
        assertThat(config).contains("enable: ${MAIL_STARTTLS_ENABLE:false}");
    }

    @Test
    void composePassesTheTimeouts() throws IOException {
        String compose = Files.readString(java.nio.file.Path.of("../docker-compose.yml"),
                StandardCharsets.UTF_8);

        assertThat(compose).contains("MAIL_CONNECT_TIMEOUT_MS");
        assertThat(compose).contains("MAIL_READ_TIMEOUT_MS");
        assertThat(compose).contains("MAIL_WRITE_TIMEOUT_MS");
    }
}
