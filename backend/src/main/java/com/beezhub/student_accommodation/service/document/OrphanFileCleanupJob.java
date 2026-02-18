package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrphanFileCleanupJob {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    @Scheduled(cron = "${app.storage.cleanup.cron:0 0 3 * * *}")
    public void cleanupOrphanedFiles() {
        log.info("Starting orphan file cleanup job");

        try (Stream<String> objectKeys = fileStorageService.listObjects()) {
            objectKeys.forEach(key -> {
                try {
                    // Check if file exists in DB. This is done one-by-one which is safe but might be slow for large datasets.
                    // Future optimization: Batch check using findByDocumentPathIn or Bloom filter.
                    if (!documentRepository.existsByDocumentPath(key)) {
                        log.info("Deleting orphaned file: type={}, keyHash={}", 
                                extractDocType(key), 
                                Integer.toHexString(key.hashCode()));
                        fileStorageService.delete(key);
                    }
                } catch (Exception e) {
                    log.error("Error processing file during cleanup: keyHash={}", Integer.toHexString(key.hashCode()), e);
                }
            });
        } catch (Exception e) {
            log.error("Error listing objects for cleanup", e);
        }

        log.info("Orphan file cleanup job completed");
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
