package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.AbstractJpaTest;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.enums.NotificationStatus;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-141: the only tests that can guard the retry query itself.
 *
 * <p>{@code NotificationRetryServiceTest} mocks the repository, so neither the widened status list
 * nor the dropped {@code retryCount < maxRetries} predicate is evaluated there. That predicate was
 * identical to {@code canRetry()}, so every row the loop saw already passed it and the branch that
 * marks a notification permanently failed could never run in production. No mock can catch its
 * reintroduction.
 *
 * <p>Runs against a real PostgreSQL container, so it is verifiable in CI only.
 */
class NotificationRetryQueryTest extends AbstractJpaTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final List<NotificationStatus> RETRYABLE =
            List.of(NotificationStatus.FAILED, NotificationStatus.RETRYING);

    private Notification persist(NotificationStatus status, int retryCount, LocalDateTime lastAttemptAt) {
        Notification n = new Notification();
        n.setRecipient("student@example.com");
        n.setSubject("Subject");
        n.setType(NotificationType.APPLICATION_APPROVED);
        n.setStatus(status);
        n.setRetryCount(retryCount);
        n.setMaxRetries(3);
        n.setLastAttemptAt(lastAttemptAt);
        n.setCreatedAt(LocalDateTime.now());
        n.setUpdatedAt(LocalDateTime.now());
        return entityManager.persistAndFlush(n);
    }

    private List<Notification> scan() {
        return notificationRepository.findRetryableNotifications(
                RETRYABLE, LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 200));
    }

    /**
     * The load-bearing one. Under the old predicate this row was filtered out, which is why the
     * terminal branch was dead code and an exhausted notification stayed FAILED forever.
     */
    @Test
    void findRetryableNotifications_rowWithRetriesExhausted_isReturnedSoItCanBeTerminated() {
        Notification exhausted = persist(NotificationStatus.FAILED, 3, LocalDateTime.now().minusHours(1));

        assertThat(scan()).extracting(Notification::getId).contains(exhausted.getId());
    }

    /** Stranded by the early return that committed RETRYING before checking the template data. */
    @Test
    void findRetryableNotifications_rowStrandedAsRetrying_isReturned() {
        Notification stranded = persist(NotificationStatus.RETRYING, 1, LocalDateTime.now().minusHours(1));

        assertThat(scan()).extracting(Notification::getId).contains(stranded.getId());
    }

    /** A never-attempted row would be dropped by SQL three-valued logic without the null check. */
    @Test
    void findRetryableNotifications_rowNeverAttempted_isReturned() {
        Notification untried = persist(NotificationStatus.FAILED, 0, null);

        assertThat(scan()).extracting(Notification::getId).contains(untried.getId());
    }

    @Test
    void findRetryableNotifications_recentlyAttemptedRow_isExcluded() {
        Notification recent = persist(NotificationStatus.FAILED, 0, LocalDateTime.now());

        assertThat(scan()).extracting(Notification::getId).doesNotContain(recent.getId());
    }

    /** Terminal rows must never come back, or the scan would grow without bound. */
    @Test
    void findRetryableNotifications_terminalAndSentRows_areExcluded() {
        Notification terminal = persist(NotificationStatus.PERMANENTLY_FAILED, 3, LocalDateTime.now().minusHours(1));
        Notification sent = persist(NotificationStatus.SENT, 0, LocalDateTime.now().minusHours(1));
        Notification pending = persist(NotificationStatus.PENDING, 0, LocalDateTime.now().minusHours(1));

        assertThat(scan()).extracting(Notification::getId)
                .doesNotContain(terminal.getId(), sent.getId(), pending.getId());
    }

    /** The cap that keeps the first pass after deploy from loading the whole historical backlog. */
    @Test
    void findRetryableNotifications_isLimitedByThePageSize() {
        for (int i = 0; i < 5; i++) {
            persist(NotificationStatus.FAILED, 0, LocalDateTime.now().minusHours(1));
        }

        assertThat(notificationRepository.findRetryableNotifications(
                RETRYABLE, LocalDateTime.now().minusMinutes(5), PageRequest.of(0, 2))).hasSize(2);
    }
}
