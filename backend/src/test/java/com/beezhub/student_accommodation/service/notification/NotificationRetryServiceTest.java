package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * GH-141: the retry job had three linked defects. A row with no template data was committed as
 * RETRYING before that was discovered, and the scan looked only at FAILED, so it was never seen
 * again. The scan also filtered on {@code retryCount < maxRetries}, the same predicate as
 * {@code canRetry()}, so the branch that marks a row permanently failed was unreachable. And the
 * database CHECK constraint did not permit that status, so the first such write would have rolled
 * back the whole batch.
 *
 * <p>What these tests can show is the loop and dispatch behaviour. What they cannot show is the
 * query itself: the repository is mocked, so neither the widened status list nor, more importantly,
 * the dropped retry-count predicate is evaluated here. That predicate removal is the change that
 * makes termination reachable in production, and only the repository integration test can guard it.
 */
@ExtendWith(MockitoExtension.class)
class NotificationRetryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationRetryService notificationRetryService;

    private Notification notification(long id, NotificationStatus status, int retryCount) {
        Notification n = new Notification();
        n.setId(id);
        n.setStatus(status);
        n.setRetryCount(retryCount);
        n.setMaxRetries(3);
        n.setType(NotificationType.APPLICATION_APPROVED);
        return n;
    }

    private void scanReturns(Notification... found) {
        ReflectionTestUtils.setField(notificationRetryService, "retryDelayMinutes", 5);
        ReflectionTestUtils.setField(notificationRetryService, "retryBatchSize", 200);
        when(notificationRepository.findRetryableNotifications(any(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(found));
    }

    // -------------------------------------------------------------------------
    // retryFailedNotifications — the scan and the loop
    // -------------------------------------------------------------------------

    /** The one assertion here that carries information about the query the service asks for. */
    @Test
    void retryFailedNotifications_scansBothFailedAndStrandedRetryingRows() {
        scanReturns();

        notificationRetryService.retryFailedNotifications();

        verify(notificationRepository).findRetryableNotifications(
                argThat(statuses -> statuses.containsAll(
                        List.of(NotificationStatus.FAILED, NotificationStatus.RETRYING))),
                any(LocalDateTime.class), any(Pageable.class));
    }

    /**
     * A row stranded in RETRYING by the old early return is now picked up and dispatched. Before the
     * fix the scan filtered on FAILED alone, so this row was invisible forever.
     */
    @Test
    void retryFailedNotifications_strandedRetryingRow_isDispatched() throws Exception {
        Notification stranded = notification(1L, NotificationStatus.RETRYING, 1);
        stranded.setRecipient("student@example.com");
        stranded.setSubject("Application Approved");
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "application-approved");
        stranded.setTemplateData(templateData);
        scanReturns(stranded);

        notificationRetryService.retryFailedNotifications();

        verify(emailService).sendTemplateEmail(eq("student@example.com"), eq("Application Approved"),
                eq("application-approved"), eq(templateData));
        assertEquals(NotificationStatus.SENT, stranded.getStatus());
    }

    /**
     * Exhaustion is checked before backoff, and this is the case that proves the order matters.
     * lastAttemptAt is now, so the backoff has not elapsed. With the backoff test first the row
     * would be skipped for not being due and would never reach a terminal state.
     */
    @Test
    void retryFailedNotifications_exhaustedRowInsideBackoffWindow_stillTerminates() {
        Notification exhausted = notification(1L, NotificationStatus.FAILED, 3);
        exhausted.setLastAttemptAt(LocalDateTime.now());
        scanReturns(exhausted);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryFailedNotifications();

        assertEquals(NotificationStatus.PERMANENTLY_FAILED, exhausted.getStatus());
        verify(notificationRepository).save(exhausted);
        verify(counter).increment();
        verifyNoInteractions(emailService);
    }

    /** The last SMTP diagnostic survives a termination for exhaustion. */
    @Test
    void retryFailedNotifications_exhaustedRow_keepsItsExistingErrorMessage() {
        Notification exhausted = notification(1L, NotificationStatus.FAILED, 3);
        exhausted.setErrorMessage("SMTP connection refused");
        scanReturns(exhausted);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryFailedNotifications();

        assertEquals("SMTP connection refused", exhausted.getErrorMessage());
    }

    @Test
    void retryFailedNotifications_rowWithinBackoffWindow_isSkippedWithoutChange() {
        Notification recent = notification(1L, NotificationStatus.FAILED, 3);
        recent.setMaxRetries(9);
        recent.setLastAttemptAt(LocalDateTime.now());
        scanReturns(recent);

        notificationRetryService.retryFailedNotifications();

        assertEquals(NotificationStatus.FAILED, recent.getStatus());
        verify(notificationRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // retryNotification — dispatch of a single row
    // -------------------------------------------------------------------------

    /**
     * The row is terminated rather than counted. The old code marked it RETRYING and incremented
     * first, then returned on discovering there was nothing to send, which is what stranded it.
     * Nothing about a row with no template data becomes sendable by waiting.
     */
    @Test
    void retryNotification_nullTemplateData_terminatesWithoutCountingAnAttempt() {
        Notification notification = notification(4L, NotificationStatus.FAILED, 0);
        notification.setTemplateData(null);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryNotification(notification);

        verifyNoInteractions(emailService);
        assertEquals(NotificationStatus.PERMANENTLY_FAILED, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
        assertEquals("templateData missing or has no templateName", notification.getErrorMessage());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void retryNotification_templateDataWithoutTemplateName_terminates() {
        Notification notification = notification(5L, NotificationStatus.FAILED, 0);
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("studentName", "Alice");
        notification.setTemplateData(templateData);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryNotification(notification);

        verifyNoInteractions(emailService);
        assertEquals(NotificationStatus.PERMANENTLY_FAILED, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
    }

    @Test
    void retryNotification_blankTemplateName_terminates() {
        Notification notification = notification(6L, NotificationStatus.FAILED, 0);
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "   ");
        notification.setTemplateData(templateData);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        notificationRetryService.retryNotification(notification);

        verifyNoInteractions(emailService);
        assertEquals(NotificationStatus.PERMANENTLY_FAILED, notification.getStatus());
    }

    @Test
    void retryNotification_withValidTemplateData_sendsEmailAndMarksSent() throws Exception {
        Notification notification = notification(2L, NotificationStatus.FAILED, 0);
        notification.setRecipient("student@example.com");
        notification.setSubject("Application Approved");

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "application-approved");
        templateData.put("studentName", "Alice");
        notification.setTemplateData(templateData);

        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(),
                ArgumentMatchers.<Map<String, Object>>any());

        notificationRetryService.retryNotification(notification);

        verify(emailService).sendTemplateEmail(
                eq("student@example.com"),
                eq("Application Approved"),
                eq("application-approved"),
                eq(templateData));
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
        assertEquals(1, notification.getRetryCount());
        // Once after markAsRetrying, once after markAsSent.
        verify(notificationRepository, times(2)).save(notification);
    }

    @Test
    void retryNotification_emailThrows_marksFailedAndSetsErrorMessage() throws Exception {
        Notification notification = notification(3L, NotificationStatus.FAILED, 1);
        notification.setType(NotificationType.ROOM_ALLOCATED);
        notification.setRecipient("student@example.com");
        notification.setSubject("Room Confirmed");

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("templateName", "allocation-confirmed");
        notification.setTemplateData(templateData);

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(),
                        ArgumentMatchers.<Map<String, Object>>any());

        notificationRetryService.retryNotification(notification);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals("SMTP connection refused", notification.getErrorMessage());
        assertEquals(2, notification.getRetryCount());
        verify(notificationRepository, times(2)).save(notification);
    }
}
