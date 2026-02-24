package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrphanFileCleanupJob {

    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;

    public int cleanupOrphanedFiles() {
        log.info("Starting orphan file cleanup job");
        AtomicInteger deletedCount = new AtomicInteger(0);

        try (Stream<String> objectKeys = fileStorageService.listObjects()) {
            objectKeys.forEach(key -> {
                try {
                    if (!documentRepository.existsByDocumentPath(key)) {
                        log.info("Deleting orphaned file: type={}, keyHash={}", 
                                extractDocType(key), 
                                Integer.toHexString(key.hashCode()));
                        fileStorageService.delete(key);
                        deletedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Error processing file during cleanup: keyHash={}", Integer.toHexString(key.hashCode()), e);
                }
            });
        } catch (Exception e) {
            log.error("Error listing objects for cleanup", e);
        }

        log.info("Orphan file cleanup job completed, deleted={}", deletedCount.get());
        return deletedCount.get();
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
