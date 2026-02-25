package com.beezhub.student_accommodation.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserRole enum tests")
class UserRoleTest {

    // ─── isAdmin() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PLATFORM_ADMIN.isAdmin() returns true")
    void isAdmin_platformAdmin_returnsTrue() {
        assertThat(UserRole.PLATFORM_ADMIN.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("ADMIN (deprecated).isAdmin() returns true during migration window")
    @SuppressWarnings("deprecation")
    void isAdmin_deprecatedAdmin_returnsTrue() {
        assertThat(UserRole.ADMIN.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("STUDENT.isAdmin() returns false")
    void isAdmin_student_returnsFalse() {
        assertThat(UserRole.STUDENT.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("BUILDING_OWNER.isAdmin() returns false")
    void isAdmin_buildingOwner_returnsFalse() {
        assertThat(UserRole.BUILDING_OWNER.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("BUILDING_STAFF.isAdmin() returns false")
    void isAdmin_buildingStaff_returnsFalse() {
        assertThat(UserRole.BUILDING_STAFF.isAdmin()).isFalse();
    }

    // ─── enumRoleOf() ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "enumRoleOf(\"{0}\") -> {1}")
    @MethodSource("validRoleStrings")
    @DisplayName("enumRoleOf resolves display names case-insensitively")
    void enumRoleOf_validDisplayName_returnsCorrectRole(String input, UserRole expected) {
        assertThat(UserRole.enumRoleOf(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("enumRoleOf unknown role throws IllegalArgumentException (C5)")
    void enumRoleOf_unknownRole_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> UserRole.enumRoleOf("SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }

    // ─── Test data ───────────────────────────────────────────────────────────

    static Stream<Arguments> validRoleStrings() {
        return Stream.of(
                Arguments.of("Student",          UserRole.STUDENT),
                Arguments.of("STUDENT",          UserRole.STUDENT),
                Arguments.of("Platform_Admin",   UserRole.PLATFORM_ADMIN),
                Arguments.of("PLATFORM_ADMIN",   UserRole.PLATFORM_ADMIN),
                Arguments.of("platform_admin",   UserRole.PLATFORM_ADMIN),
                Arguments.of("Building_Owner",   UserRole.BUILDING_OWNER),
                Arguments.of("BUILDING_OWNER",   UserRole.BUILDING_OWNER),
                Arguments.of("Building_Staff",   UserRole.BUILDING_STAFF),
                Arguments.of("BUILDING_STAFF",   UserRole.BUILDING_STAFF)
        );
    }
}
