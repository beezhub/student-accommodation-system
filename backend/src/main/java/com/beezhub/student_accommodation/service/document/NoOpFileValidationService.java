package com.beezhub.student_accommodation.service.document;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class NoOpFileValidationService implements FileValidationService {

    @Override
    public ValidationResult scanFile(InputStream input, String filename) {
        return ValidationResult.valid();
    }
}
