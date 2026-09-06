package com.beezhub.student_accommodation.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * The real upload validator.
 *
 * <p>GH-144: the only implementation of this interface returned valid() unconditionally, so no
 * extension, magic-byte, MIME or size check ran server-side at all. The single effective limit was
 * the global multipart cap.
 *
 * <p>The content type is decided from the file's own leading bytes. The client's declared type is
 * not consulted at any point, which is the property that matters: it was previously stored and
 * echoed back on download, letting the client choose the response Content-Type.
 */
@Service
@Slf4j
public class AllowListFileValidationService implements FileValidationService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final long maxFileSizeBytes;

    public AllowListFileValidationService(
            @Value("${app.documents.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public ValidationResult scanFile(InputStream input, String filename, long size) {
        if (filename == null || filename.isBlank()) {
            return ValidationResult.invalid("the file has no name");
        }
        if (size <= 0) {
            return ValidationResult.invalid("the file is empty");
        }
        if (size > maxFileSizeBytes) {
            return ValidationResult.invalid(
                    "the file is larger than the " + (maxFileSizeBytes / (1024 * 1024)) + "MB limit");
        }

        String extension = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ValidationResult.invalid("only PDF, JPG and PNG files are accepted");
        }

        byte[] head;
        try {
            head = input.readNBytes(DocumentContentTypes.sniffLength());
        } catch (IOException e) {
            return ValidationResult.invalid("the file could not be read");
        }

        String sniffed = DocumentContentTypes.sniff(head);
        if (sniffed == null) {
            return ValidationResult.invalid("the file contents are not a PDF, JPG or PNG");
        }

        // The extension and the bytes have to agree, or a PDF renamed to .png would be stored as a
        // PDF and served under a name the reviewer reads as an image.
        if (!extensionMatches(extension, sniffed)) {
            log.warn("action=uploadRejected reason=extensionContentMismatch extension={} sniffed={}",
                    extension, sniffed);
            return ValidationResult.invalid("the file contents do not match its ." + extension + " extension");
        }

        return ValidationResult.valid(sniffed);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean extensionMatches(String extension, String contentType) {
        return switch (extension) {
            case "pdf" -> "application/pdf".equals(contentType);
            case "jpg", "jpeg" -> "image/jpeg".equals(contentType);
            case "png" -> "image/png".equals(contentType);
            default -> false;
        };
    }
}
