package com.beezhub.student_accommodation;

import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * GH-141: {@code chk_notification_status} permitted only PENDING, SENT, FAILED and RETRYING, so the
 * first PERMANENTLY_FAILED write would have failed at commit. Since the whole retry pass runs in one
 * transaction, that would have rolled back every status update in the batch, reverting already-sent
 * rows to FAILED so they were emailed again on the next pass.
 *
 * <p>This never fired, because the scan filtered out exhausted rows and the terminal branch was
 * unreachable. Making it reachable is what turns a latent constraint violation into a live one, so
 * the constraint has to be widened in the same change.
 *
 * <p>Has to run here rather than in a JPA slice test: {@code AbstractJpaTest} builds its schema from
 * the entities with Flyway off, so it has no CHECK constraint to violate. Verifiable in CI only.
 */
@Transactional
class NotificationStatusConstraintIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void permanentlyFailed_isAcceptedByTheStatusCheckConstraint() {
        Notification notification = new Notification();
        notification.setRecipient("student@example.com");
        notification.setSubject("Application Approved");
        notification.setType(NotificationType.APPLICATION_APPROVED);
        notification.setStatus(NotificationStatus.PERMANENTLY_FAILED);
        notification.setRetryCount(3);
        notification.setMaxRetries(3);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());

        assertThatCode(() -> notificationRepository.saveAndFlush(notification)).doesNotThrowAnyException();
    }
}
