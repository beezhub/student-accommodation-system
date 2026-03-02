package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.ApplicationStatusChangedEvent;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@TestPropertySource(properties = {
        "async.core-pool-size=2",
        "async.max-pool-size=4",
        "async.queue-capacity=10"
})
class NotificationEventListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationEventListener notificationEventListener;

    @MockBean
    private EmailDispatchService emailDispatchService;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Application status change event is handled on the notification async thread pool")
    void handleApplicationStatusChanged_runsAsync() throws InterruptedException {
        // Arrange
        AppUser user = new AppUser();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setId(1L);

        Student student = new Student();
        student.setAppUser(user);

        Application application = new Application();
        application.setId(1L);
        application.setStudent(student);

        doAnswer(invocation -> invocation.getArgument(0)).when(notificationRepository).save(any());

        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
            return null;
        }).when(emailDispatchService).sendApplicationApprovedEmail(any(), any(), any());

        // Act
        notificationEventListener.handleApplicationStatusChanged(
                new ApplicationStatusChangedEvent(application,
                        ApplicationStatus.PENDING, ApplicationStatus.APPROVED));

        // Assert
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Async task did not complete in time");
        assertTrue(threadName.get().startsWith("notification-async-"),
                "Expected thread name to start with 'notification-async-', but was '" + threadName.get() + "'");
    }
}
