package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrphanFileCleanupJobTest {

    @Mock
    FileStorageService fileStorageService;

    @Mock
    DocumentRepository documentRepository;

    @InjectMocks
    OrphanFileCleanupJob orphanFileCleanupJob;

    @Test
    void cleanup_orphanedFiles_deleted() {
        // Mock list of files
        String orphanFile = "orphan/file.pdf";
        String validFile = "valid/file.pdf";
        when(fileStorageService.listObjects()).thenReturn(Stream.of(orphanFile, validFile));

        // Mock DB checks
        when(documentRepository.existsByDocumentPath(orphanFile)).thenReturn(false);
        when(documentRepository.existsByDocumentPath(validFile)).thenReturn(true);

        orphanFileCleanupJob.cleanupOrphanedFiles();

        // Verify orphan file deleted
        verify(fileStorageService).delete(orphanFile);
        // Verify valid file NOT deleted
        verify(fileStorageService, never()).delete(validFile);
    }

    @Test
    void cleanup_noOrphans_noDeletes() {
        // Mock list of files
        String validFile1 = "valid/file1.pdf";
        String validFile2 = "valid/file2.pdf";
        when(fileStorageService.listObjects()).thenReturn(Stream.of(validFile1, validFile2));

        // Mock DB checks
        when(documentRepository.existsByDocumentPath(validFile1)).thenReturn(true);
        when(documentRepository.existsByDocumentPath(validFile2)).thenReturn(true);

        orphanFileCleanupJob.cleanupOrphanedFiles();

        // Verify no deletions
        verify(fileStorageService, never()).delete(anyString());
    }
    
    @Test
    void cleanup_errorListingObjects_loggedAndSwallowed() {
        // Mock exception
        when(fileStorageService.listObjects()).thenThrow(new RuntimeException("MinIO error"));
        
        // Should not throw
        orphanFileCleanupJob.cleanupOrphanedFiles();
        
        verify(documentRepository, never()).existsByDocumentPath(anyString());
    }

    @Test
    void cleanup_singleFileDeletionFails_jobContinues() {
        when(fileStorageService.listObjects()).thenReturn(Stream.of("orphan1.pdf", "orphan2.pdf"));
        when(documentRepository.existsByDocumentPath(anyString())).thenReturn(false);
        doThrow(new RuntimeException("Delete failed")).when(fileStorageService).delete("orphan1.pdf");
        doNothing().when(fileStorageService).delete("orphan2.pdf");

        // Should not throw
        orphanFileCleanupJob.cleanupOrphanedFiles();

        // Job continues and deletes orphan2 despite orphan1 failure
        verify(fileStorageService).delete("orphan1.pdf");
        verify(fileStorageService).delete("orphan2.pdf");
    }
}
