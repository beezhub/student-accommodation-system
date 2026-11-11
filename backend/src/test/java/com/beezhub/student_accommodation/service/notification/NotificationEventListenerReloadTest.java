package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GH-131: handlers run on the notification executor after the publishing transaction has committed,
 * so the entity carried on the event is detached and its LAZY associations are bound to a closed
 * session. Reading {@code getStudent().getAppUser()} threw {@link LazyInitializationException}
 * before the handler's inner try block, so no {@code Notification} row was written and the retry
 * job had nothing to pick up. The email was lost, leaving only a log line.
 *
 * <p>The fix gives every handler its own transaction and re-reads the root entity into it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationEventListenerReloadTest {

    @Mock
    private NotificationPersistenceService notificationService;

    @Mock
    private EmailDispatchService emailDispatchService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private NotificationEventListener listener;

    private Application detached;
    private Application managed;
    private Notification notification;

    @BeforeEach
    void setUp() {
        AppUser appUser = AppUser.builder()
                .id(1L).email("test@example.com").firstName("John").lastName("Doe").build();
        Student student = new Student();
        student.setId(1L);
        student.setAppUser(appUser);

        // The copy on the event: its id and tenant are loaded columns, but the association is a
        // proxy on a session that has already closed.
        detached = mock(Application.class);
        when(detached.getId()).thenReturn(10L);
        when(detached.getTenantId()).thenReturn(1L);
        when(detached.getStudent())
                .thenThrow(new LazyInitializationException("could not initialize proxy - no Session"));

        managed = mock(Application.class);
        when(managed.getId()).thenReturn(10L);
        when(managed.getTenantId()).thenReturn(1L);
        when(managed.getStudent()).thenReturn(student);

        notification = Notification.builder().id(1L).subject("Test Subject").build();
        when(notificationService.createNotification(anyString(), anyString(),
                any(NotificationType.class), any(), anyLong(), anyString(), any()))
                .thenReturn(notification);
    }

    @Test
    void handleApplicationStatusChanged_entityOnEventIsDetached_reloadsItAndSendsTheEmail() throws Exception {
        when(entityManager.find(Application.class, 10L)).thenReturn(managed);

        listener.handleApplicationStatusChanged(new ApplicationStatusChangedEvent(
                detached, ApplicationStatus.PENDING, ApplicationStatus.APPROVED));

        // The managed instance is what reaches the mail layer, so its associations are readable.
        verify(emailDispatchService)
                .sendApplicationApprovedEmail(eq(managed), eq("John Doe"), eq("test@example.com"));
        verify(notificationService).markNotificationAsSent(notification);
    }

    @Test
    void handleApplicationStatusChanged_rowHasSinceBeenDeleted_fallsBackToTheDetachedCopy() {
        when(entityManager.find(Application.class, 10L)).thenReturn(null);

        // Best effort: the handler tries the copy it was given rather than swallowing the event, so a
        // fully-initialised detached entity still sends. A dead proxy fails exactly as it did before,
        // which is the floor this fix raises everything else off.
        assertThatThrownBy(() -> listener.handleApplicationStatusChanged(new ApplicationStatusChangedEvent(
                detached, ApplicationStatus.PENDING, ApplicationStatus.APPROVED)))
                .isInstanceOf(LazyInitializationException.class);

        verify(notificationService, never()).markNotificationAsSent(any());
    }

    @Test
    void everyHandlerOpensItsOwnTransaction() {
        List<Method> handlers = Arrays.stream(NotificationEventListener.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TransactionalEventListener.class))
                .toList();

        // Ten since GH-530 removed the staged-confirmation handler that sent "Allocation Confirmed" early.
        assertThat(handlers).hasSize(10);
        assertThat(handlers).allSatisfy(handler -> {
            Transactional tx = handler.getAnnotation(Transactional.class);
            assertThat(tx)
                    .as("%s reads LAZY associations off a committed transaction and needs its own",
                            handler.getName())
                    .isNotNull();
            assertThat(tx.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        });
    }
}
