package com.beezhub.student_accommodation.service.document;

import java.io.InputStream;

public interface FileValidationService {

    /**
     * Validates an uploaded file and decides the content type it should be stored as.
     *
     * @param input    the file content; the implementation reads from the start
     * @param filename the client-supplied filename, used for the extension check only
     * @param size     the declared size in bytes
     */
    ValidationResult scanFile(InputStream input, String filename, long size);
}
