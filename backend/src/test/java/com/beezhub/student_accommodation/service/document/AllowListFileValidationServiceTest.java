package com.beezhub.student_accommodation.service.document;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-144: the only implementation of this interface returned valid() unconditionally, so no
 * extension, magic-byte, MIME or size check ran server-side at all. The single effective limit was
 * the global multipart cap.
 */
class AllowListFileValidationServiceTest {

    private final AllowListFileValidationService service =
            new AllowListFileValidationService(10 * 1024 * 1024);

    private static InputStream pdf() {
        return new ByteArrayInputStream("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII));
    }

    private static InputStream png() {
        byte[] data = new byte[64];
        System.arraycopy(new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A },
                0, data, 0, 8);
        return new ByteArrayInputStream(data);
    }

    private static InputStream html() {
        return new ByteArrayInputStream("<html><script>".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void scanFile_realPdf_isAcceptedAndResolvedFromItsBytes() {
        ValidationResult result = service.scanFile(pdf(), "transcript.pdf", 13);

        assertThat(result.clean()).isTrue();
        assertThat(result.resolvedContentType()).isEqualTo("application/pdf");
    }

    @Test
    void scanFile_realPng_isAccepted() {
        assertThat(service.scanFile(png(), "photo.png", 64).resolvedContentType())
                .isEqualTo("image/png");
    }

    /** The attack shape: HTML content behind a .pdf name, so a reviewer opens it as a document. */
    @Test
    void scanFile_htmlNamedAsPdf_isRejected() {
        ValidationResult result = service.scanFile(html(), "cv.pdf", 14);

        assertThat(result.clean()).isFalse();
        assertThat(result.reason()).contains("not a PDF");
    }

    /** A real PDF under an image extension would be served under a name the reviewer misreads. */
    @Test
    void scanFile_pdfNamedAsPng_isRejectedForMismatch() {
        ValidationResult result = service.scanFile(pdf(), "scan.png", 13);

        assertThat(result.clean()).isFalse();
        assertThat(result.reason()).contains("do not match");
    }

    @Test
    void scanFile_disallowedExtension_isRejected() {
        ValidationResult result = service.scanFile(pdf(), "payload.exe", 13);

        assertThat(result.clean()).isFalse();
        assertThat(result.reason()).contains("PDF, JPG and PNG");
    }

    @Test
    void scanFile_noExtension_isRejected() {
        assertThat(service.scanFile(pdf(), "transcript", 13).clean()).isFalse();
    }

    @Test
    void scanFile_missingFilename_isRejected() {
        assertThat(service.scanFile(pdf(), null, 13).clean()).isFalse();
        assertThat(service.scanFile(pdf(), "  ", 13).clean()).isFalse();
    }

    @Test
    void scanFile_emptyFile_isRejected() {
        assertThat(service.scanFile(pdf(), "transcript.pdf", 0).clean()).isFalse();
    }

    @Test
    void scanFile_oversizeFile_isRejectedWithTheLimitInTheMessage() {
        ValidationResult result = new AllowListFileValidationService(1024)
                .scanFile(pdf(), "transcript.pdf", 2048);

        assertThat(result.clean()).isFalse();
        assertThat(result.reason()).contains("larger than");
    }

    @Test
    void scanFile_uppercaseExtension_isAccepted() {
        assertThat(service.scanFile(pdf(), "TRANSCRIPT.PDF", 13).clean()).isTrue();
    }

    @Test
    void scanFile_jpegExtensionVariant_isAccepted() {
        byte[] data = new byte[32];
        System.arraycopy(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, 0, data, 0, 3);

        assertThat(service.scanFile(new ByteArrayInputStream(data), "photo.jpeg", 32).clean()).isTrue();
    }
}
