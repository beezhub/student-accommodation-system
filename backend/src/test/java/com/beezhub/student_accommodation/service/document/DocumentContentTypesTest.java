package com.beezhub.student_accommodation.service.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GH-144: the declared content type from the multipart header was stored and echoed back on
 * download, so the client chose the response Content-Type. These pin the two properties that
 * replace it — the type comes from the bytes, and anything unrecognised is downgraded.
 */
class DocumentContentTypesTest {

    private static byte[] withHeader(byte[] magic, int totalLength) {
        byte[] data = new byte[totalLength];
        System.arraycopy(magic, 0, data, 0, magic.length);
        return data;
    }

    @Test
    void sniff_pngMagic_isDetected() {
        byte[] png = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

        assertThat(DocumentContentTypes.sniff(withHeader(png, 64))).isEqualTo("image/png");
    }

    @Test
    void sniff_jpegMagic_isDetected() {
        byte[] jpeg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

        assertThat(DocumentContentTypes.sniff(withHeader(jpeg, 64))).isEqualTo("image/jpeg");
    }

    @Test
    void sniff_pdfHeader_isDetected() {
        assertThat(DocumentContentTypes.sniff("%PDF-1.7\nbody".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo("application/pdf");
    }

    /** A byte-order mark or leading whitespace before the PDF header is common and still opens. */
    @Test
    void sniff_pdfHeaderAfterALeadingByteOrderMark_isStillDetected() {
        byte[] withBom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '%', 'P', 'D', 'F', '-' };

        assertThat(DocumentContentTypes.sniff(withBom)).isEqualTo("application/pdf");
    }

    /** PNG and JPEG signatures are defined at offset 0, so a match further in must not count. */
    @Test
    void sniff_pngMagicNotAtTheStart_isNotDetected() {
        byte[] shifted = { 0x00, (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

        assertThat(DocumentContentTypes.sniff(shifted)).isNull();
    }

    @Test
    void sniff_html_isNotDetected() {
        assertThat(DocumentContentTypes.sniff("<html><script>alert(1)</script>"
                .getBytes(StandardCharsets.US_ASCII))).isNull();
    }

    @Test
    void sniff_emptyOrNull_isNull() {
        assertThat(DocumentContentTypes.sniff(new byte[0])).isNull();
        assertThat(DocumentContentTypes.sniff(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "application/pdf", "image/jpeg", "image/png" })
    void clamp_allowedType_isKept(String contentType) {
        assertThat(DocumentContentTypes.clamp(contentType)).isEqualTo(contentType);
    }

    /** The stored-XSS vector: text/html reaching an iframe through a blob URL. */
    @Test
    void clamp_html_isDowngraded() {
        assertThat(DocumentContentTypes.clamp("text/html")).isEqualTo("application/octet-stream");
    }

    /**
     * The self-inflicted lockout: an unparseable value made the download endpoint throw on every
     * later request, so that document could never be reviewed again.
     */
    @Test
    void clamp_unparseableType_isDowngradedRatherThanPropagated() {
        assertThat(DocumentContentTypes.clamp("not/a/valid;;type"))
                .isEqualTo("application/octet-stream");
    }

    @Test
    void clamp_nullType_isDowngraded() {
        assertThat(DocumentContentTypes.clamp(null)).isEqualTo("application/octet-stream");
    }

    @Test
    void clamp_ignoresParametersAndCase() {
        assertThat(DocumentContentTypes.clamp("APPLICATION/PDF; charset=utf-8"))
                .isEqualTo("application/pdf");
    }
}
