package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.model.entity.Student;

import java.util.ArrayList;
import java.util.List;

/** The profile fields a student must fill in before applying. */
final class ApplicantProfileCheck {

    private ApplicantProfileCheck() {
    }

    /**
     * The missing fields in plain words, empty when the profile is complete.
     *
     * <p>GH-239: the emergency contact is required here rather than by a NOT NULL column, because
     * existing students have no value and none can be invented for them. Name and phone only: a
     * relationship or an email with no one to call is not a contact, but a contact without them is.
     */
    static List<String> missingFields(Student student) {
        List<String> missing = new ArrayList<>();
        if (isBlank(student.getStudentNumber())) {
            missing.add("student number");
        }
        if (isBlank(student.getPhoneNumber())) {
            missing.add("phone number");
        }
        if (student.getDateOfBirth() == null) {
            missing.add("date of birth");
        }
        if (isBlank(student.getGender())) {
            missing.add("gender");
        }
        if (isBlank(student.getEmergencyContactName())) {
            missing.add("emergency contact name");
        }
        if (isBlank(student.getEmergencyContactPhone())) {
            missing.add("emergency contact phone");
        }
        return missing;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
