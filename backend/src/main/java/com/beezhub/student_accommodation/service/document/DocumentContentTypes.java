package com.beezhub.student_accommodation.service.document;

import java.util.Set;

/**
 * Decides a document's content type from its bytes rather than from what the client claimed.
 *
 * <p>GH-144: the multipart part header went straight into object storage and was echoed back on
 * download, so the client fully controlled the response Content-Type. Two things followed. A
 * syntactically invalid value made {@code MediaType.parseMediaType} throw on every later download,
 * permanently locking that document out of review. And a part declaring {@code text/html} with a
 * {@code .pdf} filename reached an iframe through {@code bypassSecurityTrustResourceUrl}, where a
 * {@code blob:} URL of that type executes script in the admin's origin, next to their token.
 */
public final class DocumentContentTypes {

    public static final String FALLBACK = "application/octet-stream";

    /** The only types a document may be served as. Anything else is downgraded to the fallback. */
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    /** How much of the file to look at. Generous enough to find a PDF header after a BOM or junk. */
    private static final int SNIFF_LENGTH = 1024;

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] PDF_MAGIC = { 0x25, 0x50, 0x44, 0x46, 0x2D };

    private DocumentContentTypes() {
    }

    public static int sniffLength() {
        return SNIFF_LENGTH;
    }

    /**
     * The content type implied by the leading bytes, or null when they match nothing known.
     *
     * <p>PNG and JPEG are anchored at offset 0, which is where their signatures are defined to be.
     * The PDF header is searched for instead, because a byte-order mark or leading whitespace
     * before {@code %PDF-} is common and still parses in every reader.
     */
    public static String sniff(byte[] head) {
        if (head == null || head.length == 0) {
            return null;
        }
        if (startsWith(head, PNG_MAGIC)) {
            return "image/png";
        }
        if (startsWith(head, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (indexOf(head, PDF_MAGIC) >= 0) {
            return "application/pdf";
        }
        return null;
    }

    /**
     * Reduces a stored content type to one that is safe to serve.
     *
     * <p>Applied on the way out as well as on the way in, so rows written before this existed are
     * neutralised without a migration.
     */
    public static String clamp(String contentType) {
        if (contentType == null) {
            return FALLBACK;
        }
        String bare = contentType.split(";")[0].trim().toLowerCase();
        return ALLOWED.contains(bare) ? bare : FALLBACK;
    }

    public static boolean isAllowed(String contentType) {
        return contentType != null && ALLOWED.contains(contentType.split(";")[0].trim().toLowerCase());
    }

    public static Set<String> allowed() {
        return ALLOWED;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int k = 0; k < needle.length; k++) {
                if (data[i + k] != needle[k]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
