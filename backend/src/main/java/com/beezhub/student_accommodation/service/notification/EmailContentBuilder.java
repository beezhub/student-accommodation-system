package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.entity.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building common email content fragments.
 * Note: HTML-building methods have been removed. This class now only contains
 * date formatting and domain-specific utility methods used by EmailDispatchService.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailContentBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm z");

    public String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_FORMATTER);
    }

    public String formatDate(java.time.LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    /**
     * A deadline with its time and zone. An offer lapses at an exact moment, so the date alone would
     * let a student act on the last day after it has already gone.
     */
    public String formatDeadline(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.atZone(ZoneId.systemDefault()).format(DEADLINE_FORMATTER);
    }

    /**
     * The academic year the application is for (GH-495). This used to be built from the calendar,
     * so a student applying for next year was told this year.
     */
    public String academicYearOf(Application application) {
        AcademicYear year = application.getAcademicYear();
        return year != null && year.getName() != null ? year.getName() : "N/A";
    }

    /**
     * The intake the application is for, for example "Semester 2" (GH-495). This used to be
     * guessed from the month, which was wrong for Annual and Trimester intakes.
     */
    public String intakeOf(Application application) {
        Intake intake = application.getIntake();
        return intake != null && intake.getName() != null ? intake.getName() : "N/A";
    }

    public String getNextApplicationPeriod() {
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear = LocalDateTime.now().getYear();

        if (currentMonth >= 1 && currentMonth <= 6) {
            return "June " + currentYear;
        } else {
            return "January " + (currentYear + 1);
        }
    }

    public List<String> parseFeedbackPoints(String comment) {
        if (comment == null || comment.isEmpty()) {
            return List.of();
        }

        if (comment.contains(";")) {
            return List.of(comment.split(";"));
        } else if (comment.contains("\n")) {
            return List.of(comment.split("\n"));
        }

        return List.of();
    }

    public Integer extractFloorNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.length() < 2) return 1;

        try {
            String floorPart = roomNumber.substring(0, 1);
            return Integer.parseInt(floorPart);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public String determineRoomType(Room room) {
        if (room.getCapacity() == null) return "Standard";

        return switch (room.getCapacity()) {
            case 1 -> "Single";
            case 2 -> "Double";
            case 3 -> "Triple";
            case 4 -> "Quad";
            default -> "Shared (" + room.getCapacity() + " beds)";
        };
    }

    public String calculateDuration(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) return "N/A";
        if (endDate == null) {
            endDate = startDate.plusMonths(10);
        }

        long months = ChronoUnit.MONTHS.between(startDate, endDate);

        if (months == 1) return "1 month";
        if (months < 12) return months + " months";

        long years = months / 12;
        long remainingMonths = months % 12;

        if (remainingMonths == 0) {
            return years == 1 ? "1 year" : years + " years";
        }

        return years + " year" + (years > 1 ? "s" : "") + " and " +
               remainingMonths + " month" + (remainingMonths > 1 ? "s" : "");
    }

    public List<String> parseWarnings(String warningsJson) {
        if (warningsJson == null || warningsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(warningsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse warnings JSON: {}", warningsJson, e);
            return new ArrayList<>();
        }
    }
}
