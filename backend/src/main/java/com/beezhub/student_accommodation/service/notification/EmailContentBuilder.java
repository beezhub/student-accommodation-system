package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailContentBuilder {

    private final SpringTemplateEngine templateEngine;

    @Value("${app.support-email:accommodation@university.edu}")
    private String supportEmail;

    @Value("${app.university-name:University}")
    private String universityName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public String buildDocumentCommentEmailContent(String studentName, String documentName,
                                                     String documentType, String documentReference,
                                                     String commentText, String portalUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "documentName", documentName,
            "documentType", documentType,
            "documentReference", documentReference,
            "commentText", commentText,
            "portalUrl", portalUrl,
            "supportEmail", supportEmail,
            "universityName", universityName
        ));
        return templateEngine.process("email/document-comment", context);
    }

    public String buildPasswordResetEmailContent(String studentName, String resetUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "resetUrl", resetUrl
        ));
        return templateEngine.process("email/password-reset", context);
    }

    public String buildOfferDeclinedRequeueEmailContent(String studentName, String roomNumber,
                                                          String buildingName, int totalDeclineCount,
                                                          int declinesRemaining, String declineReason,
                                                          String portalUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "roomNumber", roomNumber,
            "buildingName", buildingName,
            "totalDeclineCount", totalDeclineCount,
            "declinesRemaining", declinesRemaining,
            "declineReason", declineReason,
            "portalUrl", portalUrl,
            "supportEmail", supportEmail,
            "universityName", universityName
        ));
        return templateEngine.process("email/offer-declined-requeue", context);
    }

    public String buildOfferDeclinedMaxReachedEmailContent(String studentName, int totalDeclineCount,
                                                             String applicationCode, String portalUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "totalDeclineCount", totalDeclineCount,
            "applicationCode", applicationCode,
            "portalUrl", portalUrl,
            "supportEmail", supportEmail,
            "universityName", universityName
        ));
        return templateEngine.process("email/offer-declined-max-reached", context);
    }

    public String buildOfferExpiredEmailContent(String studentName, String applicationCode,
                                                  String roomNumber, String buildingName, String portalUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "applicationCode", applicationCode,
            "roomNumber", roomNumber,
            "buildingName", buildingName,
            "portalUrl", portalUrl,
            "supportEmail", supportEmail,
            "universityName", universityName
        ));
        return templateEngine.process("email/offer-expired", context);
    }

    public String buildCheckInExpiredEmailContent(String studentName, String applicationCode,
                                                    String roomNumber, String buildingName, String portalUrl) {
        Context context = new Context();
        context.setVariables(Map.of(
            "studentName", studentName,
            "applicationCode", applicationCode,
            "roomNumber", roomNumber,
            "buildingName", buildingName,
            "portalUrl", portalUrl,
            "supportEmail", supportEmail,
            "universityName", universityName
        ));
        return templateEngine.process("email/check-in-expired", context);
    }

    public String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_FORMATTER);
    }

    public String formatDate(java.time.LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DATE_FORMATTER);
    }

    public String getCurrentAcademicYear() {
        int currentYear = LocalDateTime.now().getYear();
        return currentYear + "/" + (currentYear + 1);
    }

    public String getCurrentSemester() {
        int currentMonth = LocalDateTime.now().getMonthValue();
        return currentMonth >= 1 && currentMonth <= 6 ? "First Semester" : "Second Semester";
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
