package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EmailContentBuilderTest {

    @InjectMocks
    private EmailContentBuilder emailContentBuilder;

    @Test
    void formatDeadline_dateTime_showsTheTimeAndZone() {
        String deadline = emailContentBuilder.formatDeadline(LocalDateTime.of(2026, 11, 11, 14, 30));

        assertTrue(deadline.startsWith("11 November 2026 at 14:30 "), deadline);
    }

    /** GH-495: the application's own year and intake, not ones built from today's date. */
    @Test
    void academicYearOfAndIntakeOf_readTheApplication() {
        AcademicYear year = AcademicYear.builder().id(1L).name("2027").build();
        Application application = Application.builder()
                .academicYear(year)
                .intake(Intake.builder().id(2L).name("Semester 2").academicYear(year).build())
                .build();

        assertEquals("2027", emailContentBuilder.academicYearOf(application));
        assertEquals("Semester 2", emailContentBuilder.intakeOf(application));
    }

    @Test
    void academicYearOfAndIntakeOf_applicationWithoutThem_sayNotAvailable() {
        Application application = new Application();

        assertEquals("N/A", emailContentBuilder.academicYearOf(application));
        assertEquals("N/A", emailContentBuilder.intakeOf(application));
    }

    @Test
    void formatDeadline_null_returnsNotAvailable() {
        assertEquals("N/A", emailContentBuilder.formatDeadline(null));
    }

    @Test
    void determineRoomType_capacity1_returnsSingle() {
        Room room = new Room();
        room.setCapacity(1);
        assertEquals("Single", emailContentBuilder.determineRoomType(room));
    }

    @Test
    void determineRoomType_capacity4_returnsQuad() {
        Room room = new Room();
        room.setCapacity(4);
        assertEquals("Quad", emailContentBuilder.determineRoomType(room));
    }

    @Test
    void calculateDuration_stayDates_countsTheMonthsBetweenThem() {
        assertEquals("11 months", emailContentBuilder.calculateDuration(
                java.time.LocalDate.of(2027, 1, 15), java.time.LocalDate.of(2027, 12, 15)));
    }

    /** GH-529: no end date means no duration, never a guessed ten months. */
    @Test
    void calculateDuration_noEndDate_returnsNull() {
        assertNull(emailContentBuilder.calculateDuration(java.time.LocalDate.of(2027, 1, 15), null));
    }

    @Test
    void calculateDuration_10Months_returns10Months() {
        assertEquals("10 months", emailContentBuilder.calculateDuration(
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 11, 1)));
    }

    @Test
    void calculateDuration_1Year_returns1Year() {
        assertEquals("1 year", emailContentBuilder.calculateDuration(
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2027, 1, 1)));
    }
}
