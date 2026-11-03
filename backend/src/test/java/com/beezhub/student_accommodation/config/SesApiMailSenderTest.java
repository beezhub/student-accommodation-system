package com.beezhub.student_accommodation.config;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.GetAccountResponse;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SES has no SMTP endpoint in af-south-1, so PROD sends through the SES v2 API (GH-360). */
@ExtendWith(MockitoExtension.class)
class SesApiMailSenderTest {

    @Mock
    private SesV2Client ses;

    private SesApiMailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SesApiMailSender(ses);
        sender.setDefaultEncoding("UTF-8");
    }

    private String sentRawMessage() {
        ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(ses).sendEmail(request.capture());
        return request.getValue().content().raw().data().asString(StandardCharsets.UTF_8);
    }

    @Test
    void send_simpleMessage_handsTheRawMimeMessageToSes() {
        when(ses.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().messageId("m1").build());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@beezhub.co.za");
        message.setTo("student@example.com");
        message.setSubject("Verify your email");
        message.setText("Click the link");

        sender.send(message);

        String raw = sentRawMessage();
        assertThat(raw).contains("From: no-reply@beezhub.co.za");
        assertThat(raw).contains("To: student@example.com");
        assertThat(raw).contains("Subject: Verify your email");
        assertThat(raw).contains("Date: ");
        assertThat(raw).contains("Click the link");
    }

    @Test
    void send_htmlMimeMessage_keepsItsContentType() throws Exception {
        when(ses.sendEmail(any(SendEmailRequest.class))).thenReturn(SendEmailResponse.builder().messageId("m2").build());
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("no-reply@beezhub.co.za");
        helper.setTo("student@example.com");
        helper.setSubject("Your offer");
        helper.setText("<p>Room 12</p>", true);

        sender.send(message);

        String raw = sentRawMessage();
        assertThat(raw).contains("text/html");
        assertThat(raw).contains("<p>Room 12</p>");
    }

    /** EmailService catches mail failures; they must surface as Spring's MailException family. */
    @Test
    void send_sesFails_throwsMailSendExceptionNamingTheMessage() {
        when(ses.sendEmail(any(SendEmailRequest.class))).thenThrow(SdkClientException.create("unreachable"));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@beezhub.co.za");
        message.setTo("student@example.com");
        message.setText("x");

        assertThatThrownBy(() -> sender.send(message))
                .isInstanceOf(MailSendException.class)
                .satisfies(e -> assertThat(((MailSendException) e).getFailedMessages()).containsKey(message));
    }

    @Test
    void send_twoMessagesOneFails_sendsTheOtherAndReportsOnlyTheFailure() {
        when(ses.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SdkClientException.create("unreachable"))
                .thenReturn(SendEmailResponse.builder().messageId("m3").build());
        SimpleMailMessage first = new SimpleMailMessage();
        first.setTo("a@example.com");
        first.setText("a");
        SimpleMailMessage second = new SimpleMailMessage();
        second.setTo("b@example.com");
        second.setText("b");

        assertThatThrownBy(() -> sender.send(first, second))
                .isInstanceOf(MailSendException.class)
                .satisfies(e -> assertThat(((MailSendException) e).getFailedMessages()).containsOnlyKeys(first));
        verify(ses, times(2)).sendEmail(any(SendEmailRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testConnection_sendingEnabled_passes() {
        when(ses.getAccount(any(Consumer.class))).thenReturn(GetAccountResponse.builder().sendingEnabled(true).build());

        assertThatCode(() -> sender.testConnection()).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testConnection_sendingDisabled_fails() {
        when(ses.getAccount(any(Consumer.class))).thenReturn(GetAccountResponse.builder().sendingEnabled(false).build());

        assertThatThrownBy(() -> sender.testConnection())
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("disabled");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testConnection_sesUnreachable_fails() {
        when(ses.getAccount(any(Consumer.class))).thenThrow(SdkClientException.create("timeout"));

        assertThatThrownBy(() -> sender.testConnection())
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("unreachable");
    }

    // ── Which sender Spring builds ─────────────────────────────────────────────────────────

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(SesMailConfig.class)
            .withPropertyValues("spring.mail.host=localhost", "spring.mail.port=1025");

    @Test
    void context_sesApiTransport_usesTheSesSenderAndBootsSmtpSenderBacksOff() {
        context.withPropertyValues("app.mail.transport=ses-api", "aws.region=af-south-1")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(JavaMailSender.class);
                    assertThat(ctx.getBean(JavaMailSender.class)).isInstanceOf(SesApiMailSender.class);
                });
    }

    @Test
    void context_defaultTransport_keepsSpringBootsSmtpSender() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(JavaMailSender.class);
            assertThat(ctx.getBean(JavaMailSender.class))
                    .isExactlyInstanceOf(JavaMailSenderImpl.class);
            assertThat(ctx).doesNotHaveBean(SesV2Client.class);
        });
    }
}
