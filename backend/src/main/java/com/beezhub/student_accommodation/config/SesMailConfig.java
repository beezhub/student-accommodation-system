package com.beezhub.student_accommodation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Email through the SES API instead of SMTP, when app.mail.transport is ses-api (PROD, GH-360).
 * Spring Boot's own SMTP sender backs off because a JavaMailSender bean exists; with the default
 * transport this class does nothing and mail goes over SMTP as before.
 *
 * <p>Credentials come from the default chain, which on the PROD server is the instance role.
 */
@Configuration
@ConditionalOnProperty(name = "app.mail.transport", havingValue = "ses-api")
public class SesMailConfig {

    @Bean
    public SesV2Client sesV2Client(@Value("${app.mail.ses-region:af-south-1}") String region) {
        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    @Bean
    public JavaMailSender javaMailSender(SesV2Client sesV2Client,
                                         @Value("${spring.mail.default-encoding:UTF-8}") String encoding) {
        SesApiMailSender sender = new SesApiMailSender(sesV2Client);
        sender.setDefaultEncoding(encoding);
        return sender;
    }
}
