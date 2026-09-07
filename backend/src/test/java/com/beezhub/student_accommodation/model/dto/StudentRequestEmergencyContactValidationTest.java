package com.beezhub.student_accommodation.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * GH-239: the emergency contact's phone deliberately does not use the {@code ^[0-9]{10}$} pattern
 * that {@code StudentRequest} applies to the student's own number.
 *
 * <p>That pattern rejects an international number, and a student's family may well not be in the
 * country — exactly the situation in which this record matters most. Copying the existing pattern
 * by reflex would have made the feature unusable for the students who most need it.
 */
class StudentRequestEmergencyContactValidationTest {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    private static StudentRequest requestWithPhone(String phone) {
        StudentRequest request = new StudentRequest();
        request.setFirstName("Sam");
        request.setLastName("Student");
        request.setEmail("sam@example.com");
        request.setInstitutionId(1L);
        request.setYearOfStudyId(1L);
        request.setEmergencyContactPhone(phone);
        return request;
    }

    private static Set<String> violatedFields(StudentRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "+27821234567",
        "+27 82 123 4567",
        "0821234567",
        "+44 20 7946 0958",
        "+1 (555) 123-4567",
        "021 123 4567",
    })
    void aRealPhoneNumberIsAccepted(String phone) {
        assertThat(violatedFields(requestWithPhone(phone)))
                .doesNotContain("emergencyContactPhone");
    }

    /** Optional at this layer; {@code submitApplication} is what requires it. */
    @Test
    void anAbsentPhoneIsAcceptedHereBecauseTheGateIsElsewhere() {
        assertThat(violatedFields(requestWithPhone(""))).doesNotContain("emergencyContactPhone");
        assertThat(violatedFields(requestWithPhone(null))).doesNotContain("emergencyContactPhone");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not a phone",
        "12345",
        "+27-82-CALL-MOM",
        "<script>alert(1)</script>",
    })
    void somethingThatIsNotAPhoneNumberIsRejected(String phone) {
        assertThat(violatedFields(requestWithPhone(phone))).contains("emergencyContactPhone");
    }

    /**
     * The student's own number keeps the stricter domestic rule. Asserted so the two are visibly a
     * deliberate difference rather than one having drifted from the other.
     */
    @Test
    void theStudentsOwnNumberKeepsTheStricterDomesticRule() {
        StudentRequest request = requestWithPhone("+27 82 123 4567");
        request.setPhoneNumber("+27 82 123 4567");

        Set<String> violations = violatedFields(request);

        assertThat(violations).contains("phoneNumber");
        assertThat(violations).doesNotContain("emergencyContactPhone");
    }

    @Test
    void anInvalidEmergencyContactEmailIsRejected() {
        StudentRequest request = requestWithPhone("+27821234567");
        request.setEmergencyContactEmail("not-an-email");

        assertThat(violatedFields(request)).contains("emergencyContactEmail");
    }
}
