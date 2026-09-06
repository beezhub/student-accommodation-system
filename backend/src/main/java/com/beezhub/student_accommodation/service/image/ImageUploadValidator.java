package com.beezhub.student_accommodation.service.image;

import com.beezhub.student_accommodation.service.document.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates a building image upload and decides its content type from the file's own bytes.
 *
 * <p>Shaped after {@code AllowListFileValidationService}, which does the same job for student
 * documents, but with an images-only allow-list: a building gallery has no business accepting a
 * PDF, and unlike documents it does accept GIF and WebP (GH-219).
 *
 * <p>The client's declared content type is not consulted at any point. That is the property that
 * matters: it was previously checked against a list of strings the uploader also controlled, then
 * stored on the row and sent to object storage as the object's type.
 */
@Service
@Slf4j
public class ImageUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final long maxFileSizeBytes;

    public ImageUploadValidator(
            @Value("${app.building-images.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /** Validates the file and, when it passes, returns the content type read from its bytes. */
    public ValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.invalid("the file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return ValidationResult.invalid("the file has no name");
        }
        if (file.getSize() > maxFileSizeBytes) {
            return ValidationResult.invalid(
                    "the file is larger than the " + (maxFileSizeBytes / (1024 * 1024)) + "MB limit");
        }

        String extension = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ValidationResult.invalid("only JPG, PNG, GIF and WebP images are accepted");
        }

        byte[] head;
        try (InputStream input = file.getInputStream()) {
            head = input.readNBytes(ImageContentTypes.sniffLength());
        } catch (IOException e) {
            return ValidationResult.invalid("the file could not be read");
        }

        String sniffed = ImageContentTypes.sniff(head);
        if (sniffed == null) {
            return ValidationResult.invalid("the file contents are not a JPG, PNG, GIF or WebP image");
        }

        // The extension and the bytes have to agree, or a GIF renamed to .png is stored as a GIF
        // under a name that says otherwise, and the two disagree everywhere downstream.
        if (!extensionMatches(extension, sniffed)) {
            log.warn("action=imageUploadRejected reason=extensionContentMismatch extension={} sniffed={}",
                    extension, sniffed);
            return ValidationResult.invalid(
                    "the file contents do not match its ." + extension + " extension");
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
            case "jpg", "jpeg" -> "image/jpeg".equals(contentType);
            case "png" -> "image/png".equals(contentType);
            case "gif" -> "image/gif".equals(contentType);
            case "webp" -> "image/webp".equals(contentType);
            default -> false;
        };
    }
}
