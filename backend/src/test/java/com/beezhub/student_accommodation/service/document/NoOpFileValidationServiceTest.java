package com.beezhub.student_accommodation.service.document;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOpFileValidationServiceTest {

    private final NoOpFileValidationService service = new NoOpFileValidationService();

    @Test
    void scanFile_anyFile_returnsClean() {
        InputStream stream = new ByteArrayInputStream("content".getBytes());
        ValidationResult result = service.scanFile(stream, "file.txt");
        assertTrue(result.clean());
    }
}
