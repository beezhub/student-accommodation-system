package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.config.AsyncConfiguration;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import com.beezhub.student_accommodation.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NotificationService.class, AsyncConfiguration.class})
@TestPropertySource(properties = {
    "async.core-pool-size=2",
    "async.max-pool-size=4",
    "async.queue-capacity=10"
})
class NotificationServiceAsyncIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private EmailDispatchService emailDispatchService;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    void sendApplicationStatusNotification_runsAsync() throws InterruptedException {
        // Setup data
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

        // Stub repository
        doAnswer(invocation -> invocation.getArgument(0)).when(notificationRepository).save(any());

        // Capture thread name
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
            return null;
        }).when(emailDispatchService).sendApplicationApprovedEmail(any(), any(), any());

        // Execute
        notificationService.sendApplicationStatusNotification(application, ApplicationStatus.APPROVED);

        // Wait
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Async task did not complete in time");

        // Verify
        String name = threadName.get();
        // The executor prefix is "notification-async-"
        assertTrue(name.startsWith("notification-async-"), 
            "Expected thread name to start with 'notification-async-', but was '" + name + "'");
    }
}
