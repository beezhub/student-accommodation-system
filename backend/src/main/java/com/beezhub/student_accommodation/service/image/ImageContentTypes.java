package com.beezhub.student_accommodation.service.image;

import java.util.Locale;
import java.util.Set;

/**
 * Decides a building image's content type from its bytes rather than from what the client claimed.
 *
 * <p>GH-219: {@code BuildingImageService} stored {@code file.getContentType()} on the row and sent
 * it to object storage, checking it only against an allow-list of strings the uploader also
 * controlled. So the stored type was attacker-chosen. An unparseable value breaks retrieval for
 * that object permanently, and nothing checked the file was an image at all.
 *
 * <p>Lower severity than the same defect on student documents (GH-144), because building images are
 * served cross-origin through presigned URLs and there is no same-origin token to steal. It is
 * still the same bug.
 *
 * <p>Deliberately separate from {@code DocumentContentTypes} rather than sharing its allow-list:
 * documents may be PDFs and building images may not, while images may be GIF and WebP and documents
 * may not. Parameterising one class over two different policies would have made both harder to read
 * than the few lines of byte matching each needs.
 */
public final class ImageContentTypes {

    public static final String FALLBACK = "application/octet-stream";

    /** The only types a building image may be stored or served as. */
    private static final Set<String> ALLOWED =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** Enough to cover the longest signature below (WebP needs 12 bytes) with room to spare. */
    private static final int SNIFF_LENGTH = 64;

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] GIF87A_MAGIC = { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 };
    private static final byte[] GIF89A_MAGIC = { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 };
    private static final byte[] RIFF_MAGIC = { 0x52, 0x49, 0x46, 0x46 };
    private static final byte[] WEBP_MAGIC = { 0x57, 0x45, 0x42, 0x50 };

    private ImageContentTypes() {
    }

    public static int sniffLength() {
        return SNIFF_LENGTH;
    }

    /** The content type implied by the leading bytes, or null when they match no allowed image. */
    public static String sniff(byte[] head) {
        if (head == null || head.length == 0) {
            return null;
        }
        if (startsWith(head, 0, PNG_MAGIC)) {
            return "image/png";
        }
        if (startsWith(head, 0, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (startsWith(head, 0, GIF87A_MAGIC) || startsWith(head, 0, GIF89A_MAGIC)) {
            return "image/gif";
        }
        // A WebP file is a RIFF container whose four-byte form type sits at offset 8; the four
        // bytes between are the chunk length and vary per file.
        if (startsWith(head, 0, RIFF_MAGIC) && startsWith(head, 8, WEBP_MAGIC)) {
            return "image/webp";
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
        String bare = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        // image/jpg is not a registered type but is what several clients send, and rows already
        // carry it. Treat it as the JPEG it is rather than downgrading those to a binary blob.
        if ("image/jpg".equals(bare)) {
            return "image/jpeg";
        }
        return ALLOWED.contains(bare) ? bare : FALLBACK;
    }

    public static Set<String> allowed() {
        return ALLOWED;
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
