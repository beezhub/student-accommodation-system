package com.beezhub.student_accommodation.config;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link JavaMailSenderImpl} that hands each finished MIME message to the SES v2 API instead of
 * an SMTP server. SES has no SMTP endpoint in af-south-1 (GH-360), but its API is there.
 *
 * <p>Messages are built exactly as with SMTP: EmailService and every template are unchanged, and
 * the From header is whatever app.mail.from says (GH-356). SES reads the sender and recipients from
 * the raw message's own headers.
 */
public class SesApiMailSender extends JavaMailSenderImpl {

    private final SesV2Client ses;

    public SesApiMailSender(SesV2Client ses) {
        this.ses = ses;
    }

    @Override
    protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
        Map<Object, Exception> failed = new LinkedHashMap<>();
        for (int i = 0; i < mimeMessages.length; i++) {
            MimeMessage message = mimeMessages[i];
            try {
                if (message.getSentDate() == null) {
                    message.setSentDate(new Date());
                }
                message.saveChanges();
                ByteArrayOutputStream raw = new ByteArrayOutputStream();
                message.writeTo(raw);
                ses.sendEmail(SendEmailRequest.builder()
                        .content(EmailContent.builder()
                                .raw(RawMessage.builder().data(SdkBytes.fromByteArray(raw.toByteArray())).build())
                                .build())
                        .build());
            } catch (Exception e) {
                Object original = originalMessages != null ? originalMessages[i] : message;
                failed.put(original, e);
            }
        }
        if (!failed.isEmpty()) {
            throw new MailSendException(failed);
        }
    }

    /**
     * What Spring Boot's mail health check calls. With no SMTP server to connect to, it asks SES
     * whether this account may send.
     */
    @Override
    public void testConnection() throws MessagingException {
        try {
            if (!Boolean.TRUE.equals(ses.getAccount(r -> { }).sendingEnabled())) {
                throw new MessagingException("SES sending is disabled for this account");
            }
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("SES is unreachable: " + e.getMessage(), e);
        }
    }
}
