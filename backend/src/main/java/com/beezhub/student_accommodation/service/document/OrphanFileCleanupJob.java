package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Deletes storage objects under the student-document key space that no longer have a
 * {@code document} row.
 *
 * <p>Document keys are {@code {role}/{userId}/{docType}/{file}} (see
 * {@code DocumentLifecycleService.buildObjectPath}), so the job lists one prefix per
 * {@link UserRole} and never touches other key spaces in the shared bucket, such as
 * {@code buildings/} where {@code building_image} objects live. As a second guard every
 * candidate is also checked against {@code building_image.object_path}.
 *
 * <p>Uploads write the object before the DB row commits, so objects younger than the
 * configured grace period are skipped.
 */
@Service
@Slf4j
public class OrphanFileCleanupJob {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final Duration gracePeriod;
    private final Clock clock;

    public OrphanFileCleanupJob(FileStorageService fileStorageService,
                                DocumentRepository documentRepository,
                                BuildingImageRepository buildingImageRepository,
                                @Value("${app.storage.cleanup.grace-period-minutes:60}") long gracePeriodMinutes) {
        this(fileStorageService, documentRepository, buildingImageRepository,
                Duration.ofMinutes(gracePeriodMinutes), Clock.systemUTC());
    }

    OrphanFileCleanupJob(FileStorageService fileStorageService,
                         DocumentRepository documentRepository,
                         BuildingImageRepository buildingImageRepository,
                         Duration gracePeriod,
                         Clock clock) {
        this.fileStorageService = fileStorageService;
        this.documentRepository = documentRepository;
        this.buildingImageRepository = buildingImageRepository;
        this.gracePeriod = gracePeriod;
        this.clock = clock;
    }

    /** Key prefixes the job may delete under: one per user role, matching buildObjectPath. */
    static List<String> documentPrefixes() {
        return Arrays.stream(UserRole.values())
                .map(role -> role.getRole() + "/")
                .toList();
    }

    public int cleanupOrphanedFiles() {
        log.info("Starting orphan file cleanup job gracePeriodMinutes={}", gracePeriod.toMinutes());
        Instant cutoff = Instant.now(clock).minus(gracePeriod);
        AtomicInteger deletedCount = new AtomicInteger(0);

        for (String prefix : documentPrefixes()) {
            try (Stream<StoredObject> objects = fileStorageService.listObjects(prefix)) {
                objects.forEach(object -> {
                    if (isOrphan(object, prefix, cutoff)) {
                        deleteQuietly(object.key(), deletedCount);
                    }
                });
            } catch (Exception e) {
                log.error("Error listing objects for cleanup prefix={}", prefix, e);
            }
        }

        log.info("Orphan file cleanup job completed, deleted={}", deletedCount.get());
        return deletedCount.get();
    }

    private boolean isOrphan(StoredObject object, String prefix, Instant cutoff) {
        String key = object.key();
        try {
            if (key == null || !key.startsWith(prefix)) {
                log.warn("Skipping object outside requested prefix: prefix={} keyHash={}", prefix, hash(key));
                return false;
            }
            if (object.lastModified() == null || object.lastModified().isAfter(cutoff)) {
                log.debug("Skipping recent or undated object: keyHash={}", hash(key));
                return false;
            }
            if (documentRepository.existsByDocumentPath(key)) {
                return false;
            }
            if (buildingImageRepository.existsByObjectPath(key)) {
                log.warn("Building image found under document prefix, not deleting: keyHash={}", hash(key));
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error checking object during cleanup: keyHash={}", hash(key), e);
            return false;
        }
    }

    private void deleteQuietly(String key, AtomicInteger deletedCount) {
        try {
            log.info("Deleting orphaned file: type={}, keyHash={}", extractDocType(key), hash(key));
            fileStorageService.delete(key);
            deletedCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Error deleting file during cleanup: keyHash={}", hash(key), e);
        }
    }

    private static String hash(String key) {
        return key == null ? "null" : Integer.toHexString(key.hashCode());
    }

    private String extractDocType(String key) {
        if (key == null) return "unknown";
        String[] parts = key.split("/");
        // Format: role/userId/docType/filename
        if (parts.length >= 3) {
            return parts[2];
        }
        return "unknown";
    }
}
