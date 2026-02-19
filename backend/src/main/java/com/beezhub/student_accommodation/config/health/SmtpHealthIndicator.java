package com.beezhub.student_accommodation.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class SmtpHealthIndicator implements HealthIndicator {

    private final JavaMailSender javaMailSender;
    private final String host;
    private final int port;

    public SmtpHealthIndicator(JavaMailSender javaMailSender,
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port) {
        this.javaMailSender = javaMailSender;
        this.host = host;
        this.port = port;
    }

    @Override
    public Health health() {
        try (Socket socket = new Socket(host, port)) {
            if (socket.isConnected()) {
                return Health.up()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("status", "Connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("error", "Not connected")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("host", host)
                    .withDetail("port", port)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
