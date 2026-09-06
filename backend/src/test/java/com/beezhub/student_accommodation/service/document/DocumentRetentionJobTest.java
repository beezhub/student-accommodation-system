package com.beezhub.student_accommodation.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * GH-235: nothing expired uploaded documents, so they accumulated for ever.
 *
 * <p>The query is asserted against a real database in {@code DocumentRetentionQueryTest}; these
 * cover the parts of the decision that live in Java — the cutoff, the dry run, the batching, and
 * that the object is deleted through the after-commit event rather than inline.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentRetentionJobTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 6, 3, 30);

    private static final Clock FIXED =
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock private DocumentRepository documentRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DocumentRetentionJob job(boolean dryRun) {
        return new DocumentRetentionJob(documentRepository, eventPublisher, directTemplate(),
                Period.ofMonths(12), dryRun, FIXED);
    }

    // ── The cutoff ──────────────────────────────────────────────────────────────────────────

    @Test
    void theCutoffIsTheRetentionPeriodBeforeNow() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(List.of());

        job(false).expireDocumentsPastRetention();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(documentRepository).findExpiredByRetention(any(), any(), cutoff.capture(), any());
        assertThat(cutoff.getValue()).isEqualTo(NOW.minusMonths(12));
    }

    /**
     * CHECKED_IN is terminal in the application state machine, so a pack kept while any
     * application sits there would never expire for anyone who took up a room. Residency is the
     * allocation's to report instead.
     */
    @Test
    void aCheckedInApplicationDoesNotByItselfKeepAPack() {
        assertThat(DocumentRetentionJob.IN_USE_APPLICATION_STATUSES)
                .doesNotContain(ApplicationStatus.CHECKED_IN)
                .containsAll(ApplicationStatus.LIVE_STATUSES.stream()
                        .filter(status -> status != ApplicationStatus.CHECKED_IN)
                        .toList());
        assertThat(DocumentRetentionJob.HELD_ALLOCATION_STATUSES)
                .containsExactlyInAnyOrder(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED,
                        AllocationStatus.ACTIVE);
    }

    // ── Dry run ─────────────────────────────────────────────────────────────────────────────

    /** The default. Deletion is irreversible and the first run acts on the whole backlog. */
    @Test
    void aDryRunReportsWithoutDeletingAnything() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(3));

        assertThat(job(true).expireDocumentsPastRetention()).isEqualTo(3);

        verify(documentRepository, never()).deleteById(anyLong());
        verifyNoInteractions(eventPublisher);
    }

    /** Nothing is deleted, so a second round would return the same page for ever. */
    @Test
    void aDryRunOverAFullPageStopsAfterOneRound() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(DocumentRetentionJob.BATCH_SIZE));

        assertThat(job(true).expireDocumentsPastRetention())
                .isEqualTo(DocumentRetentionJob.BATCH_SIZE);

        verify(documentRepository).findExpiredByRetention(any(), any(), any(), any());
    }

    // ── Deleting ────────────────────────────────────────────────────────────────────────────

    @Test
    void deletingARowSchedulesItsObjectForRemovalAfterCommit() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(1));

        assertThat(job(false).expireDocumentsPastRetention()).isEqualTo(1);

        verify(documentRepository).deleteById(1L);
        ArgumentCaptor<StoredFileDeletionEvent> event =
                ArgumentCaptor.forClass(StoredFileDeletionEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().objectPath()).isEqualTo("STUDENT/1/ID_DOCUMENT/f.pdf");
    }

    /** A row whose object was already removed still has to go; there is nothing left to delete. */
    @Test
    void aRowWithNoObjectPathIsDeletedWithoutAnEvent() {
        Document orphaned = document(1L);
        orphaned.setDocumentPath(null);
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(List.of(orphaned));

        assertThat(job(false).expireDocumentsPastRetention()).isEqualTo(1);

        verify(documentRepository).deleteById(1L);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void anEmptyResultDeletesNothingAndReportsZero() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(List.of());

        assertThat(job(false).expireDocumentsPastRetention()).isZero();

        verifyNoInteractions(eventPublisher);
    }

    // ── Batching ────────────────────────────────────────────────────────────────────────────

    /** A full page means there may be more, so the query is re-run until a short page comes back. */
    @Test
    void aBacklogLargerThanOnePageIsProcessedOverSeveralRounds() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(DocumentRetentionJob.BATCH_SIZE))
                .thenReturn(documents(DocumentRetentionJob.BATCH_SIZE))
                .thenReturn(documents(5));

        assertThat(job(false).expireDocumentsPastRetention())
                .isEqualTo(2 * DocumentRetentionJob.BATCH_SIZE + 5);
    }

    /** A query that never stops returning a full page must not spin the scheduler thread for ever. */
    @Test
    void anEndlessResultStopsAtTheBatchCeiling() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(DocumentRetentionJob.BATCH_SIZE));

        assertThat(job(false).expireDocumentsPastRetention())
                .isEqualTo(DocumentRetentionJob.MAX_BATCHES * DocumentRetentionJob.BATCH_SIZE);
    }

    /** The page is re-read rather than offset, because the deleted rows have left the result set. */
    @Test
    void everyRoundReadsTheFirstPage() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(documents(DocumentRetentionJob.BATCH_SIZE))
                .thenReturn(documents(1));

        job(false).expireDocumentsPastRetention();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(documentRepository, org.mockito.Mockito.times(2))
                .findExpiredByRetention(any(), any(), any(), pageable.capture());
        assertThat(pageable.getAllValues()).allMatch(page -> page.getPageNumber() == 0);
    }

    @Test
    void theQueryAsksForTheStatusSetsTheJobDefines() {
        when(documentRepository.findExpiredByRetention(any(), any(), any(), any()))
                .thenReturn(List.of());

        job(false).expireDocumentsPastRetention();

        ArgumentCaptor<Collection<ApplicationStatus>> applications =
                ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<AllocationStatus>> allocations =
                ArgumentCaptor.forClass(Collection.class);
        verify(documentRepository).findExpiredByRetention(
                applications.capture(), allocations.capture(), any(), any());
        assertThat(applications.getValue())
                .isEqualTo(DocumentRetentionJob.IN_USE_APPLICATION_STATUSES);
        assertThat(allocations.getValue())
                .isEqualTo(DocumentRetentionJob.HELD_ALLOCATION_STATUSES);
    }

    // ── Fixtures ────────────────────────────────────────────────────────────────────────────

    private static List<Document> documents(int count) {
        List<Document> documents = new ArrayList<>();
        IntStream.rangeClosed(1, count).forEach(id -> documents.add(document((long) id)));
        return documents;
    }

    private static Document document(Long id) {
        Student student = new Student();
        student.setId(id);

        Document document = new Document();
        document.setId(id);
        document.setStudent(student);
        document.setDocumentPath("STUDENT/" + id + "/ID_DOCUMENT/f.pdf");
        document.setCreatedAt(NOW.minusYears(3));
        return document;
    }

    /** Runs the callback inline, so the job's batching is what is under test, not Spring's TX. */
    private static TransactionTemplate directTemplate() {
        return new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }

            @Override
            public void executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action) {
                action.accept(new SimpleTransactionStatus());
            }
        };
    }
}
