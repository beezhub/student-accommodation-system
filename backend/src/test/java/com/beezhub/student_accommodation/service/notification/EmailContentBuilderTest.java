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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void extractFloorNumber_validRoomNumber_returnsFloor() {
        assertEquals(1, emailContentBuilder.extractFloorNumber("101"));
        assertEquals(2, emailContentBuilder.extractFloorNumber("205"));
    }

    @Test
    void extractFloorNumber_invalidRoomNumber_returns1() {
        assertEquals(1, emailContentBuilder.extractFloorNumber("Invalid"));
        assertEquals(1, emailContentBuilder.extractFloorNumber(""));
        assertEquals(1, emailContentBuilder.extractFloorNumber(null));
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
    void calculateDuration_10Months_returns10Months() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 0, 0);
        assertEquals("10 months", emailContentBuilder.calculateDuration(start, end));
    }

    @Test
    void calculateDuration_1Year_returns1Year() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2027, 1, 1, 0, 0);
        assertEquals("1 year", emailContentBuilder.calculateDuration(start, end));
    }

    @Test
    void parseWarnings_validJson_returnsList() {
        String json = "[\"Warning 1\", \"Warning 2\"]";
        List<String> result = emailContentBuilder.parseWarnings(json);
        assertEquals(2, result.size());
        assertEquals("Warning 1", result.get(0));
    }

    @Test
    void parseWarnings_invalidJson_returnsEmptyList() {
        String json = "Invalid JSON";
        List<String> result = emailContentBuilder.parseWarnings(json);
        assertTrue(result.isEmpty());
    }
}
