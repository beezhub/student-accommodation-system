package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanFileCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-09-05T03:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(60);
    private static final String STUDENT_PREFIX = UserRole.STUDENT.getRole() + "/";
    private static final Instant OLD = NOW.minus(Duration.ofDays(2));
    private static final Instant RECENT = NOW.minus(Duration.ofMinutes(5));

    @Mock
    FileStorageService fileStorageService;

    @Mock
    DocumentRepository documentRepository;

    @Mock
    BuildingImageRepository buildingImageRepository;

    OrphanFileCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new OrphanFileCleanupJob(fileStorageService, documentRepository, buildingImageRepository,
                GRACE, Clock.fixed(NOW, ZoneOffset.UTC));
        // Every role prefix is listed; the ones a test does not stub are empty.
        lenient().when(fileStorageService.listObjects(anyString())).thenReturn(Stream.empty());
    }

    private StoredObject student(String rest, Instant lastModified) {
        return new StoredObject(STUDENT_PREFIX + rest, lastModified);
    }

    @Test
    void cleanup_orphanedDocument_deleted() {
        StoredObject orphan = student("1/ID/orphan.pdf", OLD);
        StoredObject valid = student("1/ID/valid.pdf", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(orphan, valid));
        when(documentRepository.existsByDocumentPath(orphan.key())).thenReturn(false);
        when(documentRepository.existsByDocumentPath(valid.key())).thenReturn(true);
        when(buildingImageRepository.existsByObjectPath(orphan.key())).thenReturn(false);

        int deleted = job.cleanupOrphanedFiles();

        assertThat(deleted).isEqualTo(1);
        verify(fileStorageService).delete(orphan.key());
        verify(fileStorageService, never()).delete(valid.key());
    }

    @Test
    void cleanup_listsOnlyDocumentRolePrefixes_neverWholeBucket() {
        job.cleanupOrphanedFiles();

        for (UserRole role : UserRole.values()) {
            verify(fileStorageService).listObjects(role.getRole() + "/");
        }
        verify(fileStorageService, never()).listObjects("");
        verify(fileStorageService, never()).listObjects("buildings/");
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void cleanup_objectOutsideRequestedPrefix_notDeleted() {
        // A storage backend that ignores the prefix must still never remove building images.
        StoredObject buildingImage = new StoredObject("buildings/7/abc_photo.jpg", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(buildingImage));

        int deleted = job.cleanupOrphanedFiles();

        assertThat(deleted).isZero();
        verify(fileStorageService, never()).delete(anyString());
        verify(documentRepository, never()).existsByDocumentPath(anyString());
    }

    @Test
    void cleanup_keyTrackedAsBuildingImage_notDeleted() {
        StoredObject key = student("1/ID/shared.jpg", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(key));
        when(documentRepository.existsByDocumentPath(key.key())).thenReturn(false);
        when(buildingImageRepository.existsByObjectPath(key.key())).thenReturn(true);

        int deleted = job.cleanupOrphanedFiles();

        assertThat(deleted).isZero();
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void cleanup_objectWithinGracePeriod_notDeleted() {
        StoredObject inFlight = student("1/ID/uploading.pdf", RECENT);
        StoredObject exactlyAtCutoff = student("1/ID/edge.pdf", NOW.minus(GRACE));
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(inFlight, exactlyAtCutoff));
        when(documentRepository.existsByDocumentPath(exactlyAtCutoff.key())).thenReturn(false);
        when(buildingImageRepository.existsByObjectPath(exactlyAtCutoff.key())).thenReturn(false);

        int deleted = job.cleanupOrphanedFiles();

        assertThat(deleted).isEqualTo(1);
        verify(fileStorageService, never()).delete(inFlight.key());
        verify(fileStorageService).delete(exactlyAtCutoff.key());
        verify(documentRepository, never()).existsByDocumentPath(inFlight.key());
    }

    @Test
    void cleanup_objectWithoutLastModified_notDeleted() {
        StoredObject undated = student("1/ID/undated.pdf", null);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(undated));

        assertThat(job.cleanupOrphanedFiles()).isZero();
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void cleanup_noOrphans_noDeletes() {
        StoredObject a = student("1/ID/a.pdf", OLD);
        StoredObject b = student("2/ID/b.pdf", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(a, b));
        when(documentRepository.existsByDocumentPath(anyString())).thenReturn(true);

        assertThat(job.cleanupOrphanedFiles()).isZero();
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    void cleanup_errorListingOnePrefix_loggedAndOtherPrefixesStillProcessed() {
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenThrow(new RuntimeException("MinIO error"));
        String adminPrefix = UserRole.PLATFORM_ADMIN.getRole() + "/";
        StoredObject adminOrphan = new StoredObject(adminPrefix + "9/ID/x.pdf", OLD);
        when(fileStorageService.listObjects(adminPrefix)).thenReturn(Stream.of(adminOrphan));
        when(documentRepository.existsByDocumentPath(adminOrphan.key())).thenReturn(false);
        when(buildingImageRepository.existsByObjectPath(adminOrphan.key())).thenReturn(false);

        assertThat(job.cleanupOrphanedFiles()).isEqualTo(1);
        verify(fileStorageService).delete(adminOrphan.key());
    }

    @Test
    void cleanup_singleFileDeletionFails_jobContinues() {
        StoredObject o1 = student("1/ID/orphan1.pdf", OLD);
        StoredObject o2 = student("1/ID/orphan2.pdf", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(o1, o2));
        when(documentRepository.existsByDocumentPath(anyString())).thenReturn(false);
        when(buildingImageRepository.existsByObjectPath(anyString())).thenReturn(false);
        doThrow(new RuntimeException("Delete failed")).when(fileStorageService).delete(o1.key());

        int deleted = job.cleanupOrphanedFiles();

        assertThat(deleted).isEqualTo(1);
        verify(fileStorageService).delete(o1.key());
        verify(fileStorageService).delete(o2.key());
    }

    @Test
    void cleanup_repositoryCheckFails_objectKeptAndJobContinues() {
        StoredObject flaky = student("1/ID/flaky.pdf", OLD);
        StoredObject orphan = student("1/ID/orphan.pdf", OLD);
        when(fileStorageService.listObjects(STUDENT_PREFIX)).thenReturn(Stream.of(flaky, orphan));
        when(documentRepository.existsByDocumentPath(flaky.key())).thenThrow(new RuntimeException("DB down"));
        when(documentRepository.existsByDocumentPath(orphan.key())).thenReturn(false);
        when(buildingImageRepository.existsByObjectPath(orphan.key())).thenReturn(false);

        assertThat(job.cleanupOrphanedFiles()).isEqualTo(1);
        verify(fileStorageService, never()).delete(flaky.key());
        verify(fileStorageService).delete(orphan.key());
    }
}
