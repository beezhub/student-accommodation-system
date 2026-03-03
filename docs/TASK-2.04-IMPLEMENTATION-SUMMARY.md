I have addressed all the blocking and must-fix issues from the code review.

**Summary of Changes:**

1.  **Decomposed `DocumentService`**:
    -   Extracted `DocumentCommentService` to handle comment-related logic, reducing `DocumentService` size and dependencies.
    -   Updated `DocumentService` and `DocumentController` to use `DocumentCommentService`.

2.  **Fixed Security Vulnerabilities**:
    -   **Path Traversal (BLOCK-4)**: Updated `buildObjectPath` in `DocumentService` to use `UUID` for file keys and sanitize extensions, preventing path traversal attacks.
    -   **PII Logging (MUST-2, MUST-4)**:
        -   Updated `OrphanFileCleanupJob` to log sanitized file info (type + hash) instead of raw paths.
        -   Removed dead `studentEmail` variable and PII logging in comment creation logic.

3.  **Fixed Logic & Error Handling (BLOCK-3, BLOCK-5)**:
    -   Created `FileValidationException` and mapped it to HTTP 422 in `RestResponseExceptionHandler`.
    -   Updated `DocumentService` to throw `FileValidationException` on validation failure instead of `FileStorageException`.
    -   Removed banned `catch (Exception e)` pattern in favor of specific exception handling.
    -   Added `@Transactional` to `uploadDocuments` and `uploadSingleDocument`.

4.  **Standards & Cleanup**:
    -   Externalized cron expression in `OrphanFileCleanupJob` (MUST-3).
    -   Refactored `updateDocumentStatus` to be cleaner and use helper methods (MUST-5).
    -   Added missing constructor to `DocumentCommentNotFoundException` (MUST-6).

5.  **Testing**:
    -   Updated `DocumentServiceTest` to reflect changes (new exception type, new dependency).
    -   Created `DocumentCommentServiceTest` to cover the extracted logic.
    -   Verified changes with existing tests (`DocumentServiceTest`, `OrphanFileCleanupJobTest`).
