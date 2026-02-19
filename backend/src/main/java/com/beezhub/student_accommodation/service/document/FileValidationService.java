package com.beezhub.student_accommodation.service.document;

import java.io.InputStream;

public interface FileValidationService {
    ValidationResult scanFile(InputStream input, String filename);
}
