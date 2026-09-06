package com.beezhub.student_accommodation.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * One-way hashing for high-entropy bearer tokens and for pseudonymising identifiers in logs.
 *
 * <p>GH-154: password-reset and email-verification tokens were stored in plaintext, so anyone with
 * read access to the database held every live reset token and could take over any account that had
 * requested a reset. They are now stored hashed: the emailed link keeps the plaintext, and the
 * lookup hashes what it is given before comparing.
 *
 * <p>SHA-256 rather than BCrypt, deliberately. These are random 122-bit UUIDs, not passwords, so
 * there is nothing to brute-force and no salt is needed — and a per-row salt would make the
 * lookup-by-token impossible without scanning the table. Password hashing stays with BCrypt.
 */
public final class SecureTokenHasher {

    /** Enough to be collision-free in a log corpus while staying readable. */
    private static final int PSEUDONYM_LENGTH = 12;

    private SecureTokenHasher() {
    }

    /** Deterministic, so a token can be looked up by its hash. */
    public static String hash(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        return HexFormat.of().formatHex(sha256(rawToken));
    }

    /**
     * A stable, non-reversible stand-in for an identifier in logs.
     *
     * <p>The MDC key named {@code userId} carried the user's email address, so every log line in
     * every request logged PII, against the house rule. This keeps log lines groupable by user
     * without the logs holding an email address.
     */
    public static String pseudonym(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        return HexFormat.of().formatHex(sha256(identifier)).substring(0, PSEUDONYM_LENGTH);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every JVM.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
