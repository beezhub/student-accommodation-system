package com.beezhub.student_accommodation.service.document;

public record ValidationResult(boolean clean, String reason) {
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult invalid(String reason) {
        return new ValidationResult(false, reason);
    }
}
