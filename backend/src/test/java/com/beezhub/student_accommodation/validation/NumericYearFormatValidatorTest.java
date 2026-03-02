package com.beezhub.student_accommodation.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NumericYearFormatValidatorTest {

    private NumericYearFormatValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NumericYearFormatValidator();
        validator.initialize(null);
    }

    @Test
    void isValid_nullValue_returnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_blankValue_returnsTrue() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void isValid_emptyString_returnsTrue() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3", "10"})
    void isValid_positiveInteger_returnsTrue(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @Test
    void isValid_zeroInteger_returnsFalse() {
        assertThat(validator.isValid("0", null)).isFalse();
    }

    @Test
    void isValid_negativeInteger_returnsFalse() {
        assertThat(validator.isValid("-1", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"first", "second", "third", "fourth", "1st", "2nd", "3rd", "4th", "one", "two"})
    void isValid_ordinalPatterns_returnsTrue(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Year 3", "Level 2", "Semester 1", "year 4"})
    void isValid_embeddedNumber_returnsTrue(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"final year", "postgrad", "masters", "abc"})
    void isValid_noNumberNoOrdinal_returnsFalse(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void isValid_caseInsensitiveOrdinal_returnsTrue() {
        assertThat(validator.isValid("FIRST", null)).isTrue();
        assertThat(validator.isValid("Second", null)).isTrue();
    }
}
