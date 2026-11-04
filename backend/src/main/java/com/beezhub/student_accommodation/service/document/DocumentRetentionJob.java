package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.config.TransactionTemplateConfig;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.event.StoredFileDeletionEvent;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Deletes student documents that have outlived their retention period (GH-235).
 *
 * <p>The period is measured from the application the pack last supported closing, not from the
 * upload: twelve months after a student's last application reaches a terminal status, the pack
 * goes. A student with a live application anywhere keeps everything.
 *
 * <h2>Why this is a job and not a bucket lifecycle rule</h2>
 *
 * <p>The issue asks for a bucket lifecycle policy. A lifecycle rule expires objects by age, prefix
 * or tag — all properties of the object. "Twelve months after the application closes" is a
 * property of the database, and the same object may still be supporting a live application at
 * another company. No rule over object age can express that without deleting documents a student
 * still needs, so the decision lives here, where the applications are, and the bucket carries no
 * expiry rule over the document prefixes. {@code docs/BACKUP_AND_RESTORE.md} records the reasoning
 * and the one lifecycle rule that is safe to set.
 *
 * <h2>Dry run</h2>
 *
 * <p>Ships reporting-only. Deletion is irreversible — the bucket is unversioned, so recovery means
 * going to backups — and the first run on an existing deployment acts on the whole backlog at once.
 * So the job logs and counts what it would delete until an operator sets
 * {@code app.storage.retention.dry-run=false}, having seen what that count actually is.
 *
 * <p>Tenancy: no scoping, deliberately. The query has to see every company's applications to know
 * whether a pack is still in use; see {@link DocumentRepository#findExpiredByRetention}. On a
 * scheduler thread there is no {@code TenantContext}, so {@code TenantFilterAspect} leaves the
 * filter off.
 */
@Service
@Slf4j
public class DocumentRetentionJob {

    /**
     * Application statuses under which a pack is still in use: {@code LIVE_STATUSES} without
     * {@code CHECKED_IN}.
     *
     * <p>{@code CHECKED_IN} is terminal in the application state machine — nothing moves an
     * application out of it, not even the student moving out — so keeping a pack while any
     * application sits there would exempt every student who ever took up a room, which is most of
     * the ones this rule exists for. Residency is tracked by the allocation instead; see
     * {@link #HELD_ALLOCATION_STATUSES}.
     */
    public static final Set<ApplicationStatus> IN_USE_APPLICATION_STATUSES;

    static {
        EnumSet<ApplicationStatus> inUse = EnumSet.copyOf(ApplicationStatus.LIVE_STATUSES);
        inUse.remove(ApplicationStatus.CHECKED_IN);
        IN_USE_APPLICATION_STATUSES = Collections.unmodifiableSet(inUse);
    }

    /**
     * Allocation statuses that hold a bed: an offer out, an offer accepted, or a tenancy running.
     *
     * <p>This is what says a student is still resident. It goes to COMPLETED or TERMINATED when
     * they move out, and that is the moment the twelve months start for someone who lived here.
     */
    public static final Set<AllocationStatus> HELD_ALLOCATION_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(AllocationStatus.OFFERED, AllocationStatus.ACCEPTED, AllocationStatus.ACTIVE));

    /**
     * Rows examined per round. The query is re-run rather than offset, because the previous round's
     * rows have left the result set.
     */
    static final int BATCH_SIZE = 200;

    /** Ceiling on rounds, so a query that stops making progress cannot spin for ever. */
    static final int MAX_BATCHES = 50;

    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final Period retentionPeriod;
    private final boolean dryRun;
    private final Clock clock;

    // Required: two constructors, so Spring cannot infer which to use and falls back to a
    // default one that does not exist. OrphanFileCleanupJob marks its public constructor the
    // same way.
    @Autowired
    public DocumentRetentionJob(
            DocumentRepository documentRepository,
            ApplicationEventPublisher eventPublisher,
            @Qualifier(TransactionTemplateConfig.REQUIRES_NEW) TransactionTemplate requiresNewTransactionTemplate,
            @Value("${app.storage.retention.document-months:12}") int retentionMonths,
            @Value("${app.storage.retention.dry-run:true}") boolean dryRun) {
        this(documentRepository, eventPublisher, requiresNewTransactionTemplate,
                // The cutoff is compared with stored timestamps, which carry the JVM's zone.
                Period.ofMonths(retentionMonths), dryRun, Clock.systemDefaultZone());
    }

    DocumentRetentionJob(DocumentRepository documentRepository,
                         ApplicationEventPublisher eventPublisher,
                         TransactionTemplate requiresNewTransactionTemplate,
                         Period retentionPeriod,
                         boolean dryRun,
                         Clock clock) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.requiresNewTransactionTemplate = requiresNewTransactionTemplate;
        this.retentionPeriod = retentionPeriod;
        this.dryRun = dryRun;
        this.clock = clock;
    }

    /**
     * @return documents deleted, or in dry-run mode the number that would have been
     */
    public int expireDocumentsPastRetention() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minus(retentionPeriod);
        log.info("job=documentRetention action=start retention={} cutoff={} dryRun={}",
                retentionPeriod, cutoff, dryRun);

        int total = 0;
        for (int round = 0; round < MAX_BATCHES; round++) {
            int processed = expireOneBatch(cutoff);
            total += processed;
            // A dry run deletes nothing, so the next round would return the same page for ever.
            if (dryRun || processed < BATCH_SIZE) {
                return report(total, dryRun && processed == BATCH_SIZE);
            }
        }
        log.warn("job=documentRetention action=batchLimitReached deleted={} — the remainder is left "
                + "for the next run", total);
        return total;
    }

    private int report(int total, boolean truncated) {
        if (dryRun) {
            log.warn("job=documentRetention action=dryRun wouldDelete={}{} — set "
                            + "app.storage.retention.dry-run=false to act on this",
                    total, truncated ? "+ (first batch only)" : "");
        } else {
            log.info("job=documentRetention action=complete deleted={}", total);
        }
        return total;
    }

    /**
     * One batch in its own transaction, so a failure part-way through a long run keeps the rounds
     * that already committed.
     *
     * <p>A template rather than {@code @Transactional}: the caller is a method on this same bean,
     * and a self-invocation never reaches the proxy, so the annotation would silently do nothing.
     */
    int expireOneBatch(LocalDateTime cutoff) {
        List<Document> expired = requiresNewTransactionTemplate.execute(status ->
                documentRepository.findExpiredByRetention(
                        IN_USE_APPLICATION_STATUSES, HELD_ALLOCATION_STATUSES, cutoff,
                        PageRequest.of(0, BATCH_SIZE)));
        if (expired == null || expired.isEmpty()) {
            return 0;
        }

        if (dryRun) {
            expired.forEach(document -> log.info(
                    "job=documentRetention action=wouldDelete documentId={} studentId={} uploaded={}",
                    document.getId(), studentIdOf(document), document.getCreatedAt()));
            return expired.size();
        }

        requiresNewTransactionTemplate.executeWithoutResult(status ->
                expired.forEach(this::deleteOne));
        return expired.size();
    }

    private void deleteOne(Document document) {
        String objectPath = document.getDocumentPath();
        documentRepository.deleteById(document.getId());
        log.info("job=documentRetention action=deleted documentId={} studentId={} uploaded={}",
                document.getId(), studentIdOf(document), document.getCreatedAt());
        if (objectPath != null) {
            // After commit, matching every other deletion path: an object outliving a rolled-back
            // delete is recoverable, a row pointing at a deleted object is not (GH-145).
            eventPublisher.publishEvent(new StoredFileDeletionEvent(objectPath));
        }
    }

    /** Ids only. The document name is the student's own file name and is not logged. */
    private static Long studentIdOf(Document document) {
        return document.getStudent() == null ? null : document.getStudent().getId();
    }

    public Period retentionPeriod() {
        return retentionPeriod;
    }

    public boolean isDryRun() {
        return dryRun;
    }
}
