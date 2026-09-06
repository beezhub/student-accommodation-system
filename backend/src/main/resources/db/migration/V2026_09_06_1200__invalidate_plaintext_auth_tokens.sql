-- GH-154: password-reset and email-verification tokens were stored in plaintext.
--
-- Anyone with read access to the database held every live reset token and could take over any
-- account that had requested a reset. They are now stored as a SHA-256 hash: the emailed link
-- carries the plaintext, and the lookup hashes what it is given before comparing.
--
-- Existing rows hold plaintext, which no longer matches anything the new lookup computes, so they
-- are deleted rather than converted. Converting in place would need pgcrypto, and leaving them
-- would keep the plaintext readable for the rest of their lifetime for no benefit, since the code
-- can no longer match them either way.
--
-- The cost is small and self-correcting: both token types expire in hours, and anyone holding an
-- unused link simply requests another. The endpoints that issue them report the same message
-- whatever happens, so nothing leaks about which accounts were affected.

DELETE FROM password_reset_token;
DELETE FROM email_verification_token;
