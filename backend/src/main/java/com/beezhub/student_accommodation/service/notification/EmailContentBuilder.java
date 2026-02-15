package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class EmailContentBuilder {

    @Value("${app.support-email:accommodation@university.edu}")
    private String supportEmail;

    @Value("${app.university-name:University}")
    private String universityName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public String buildDocumentCommentEmailContent(String studentName, String documentName,
                                                     String documentType, String documentReference,
                                                     String commentText, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #667eea; padding: 15px; margin: 20px 0; }
                        .comment-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Document Requires Attention</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>An administrator has added a comment to your document that requires your attention.</p>
                            <div class="info-box">
                                <strong>Document Details:</strong>
                                <ul>
                                    <li><strong>Document:</strong> %s</li>
                                    <li><strong>Type:</strong> %s</li>
                                    <li><strong>Reference:</strong> %s</li>
                                </ul>
                            </div>
                            <div class="comment-box">
                                <strong>Admin Comment:</strong>
                                <p>%s</p>
                            </div>
                            <p>Please review the comment and take appropriate action. You may need to resubmit the document with the requested changes.</p>
                            <center>
                                <a href="%s" class="button">View Documents</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>If you have any questions, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, documentName, documentType, documentReference,
                              commentText, portalUrl, supportEmail, universityName);
    }

    public String buildPasswordResetEmailContent(String studentName, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Password Reset Request</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>We received a request to reset your password for your Student Accommodation System account.</p>
                            <p>Click the button below to reset your password:</p>
                            <center>
                                <a href="%s" class="button">Reset Password</a>
                            </center>
                            <div class="warning">
                                <strong>⚠️ Security Notice:</strong>
                                <ul>
                                    <li>This link will expire in 1 hour</li>
                                    <li>If you didn't request this reset, please ignore this email</li>
                                    <li>Never share this link with anyone</li>
                                </ul>
                            </div>
                            <p>If the button doesn't work, copy and paste this link into your browser:</p>
                            <p style="word-break: break-all; color: #667eea;">%s</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>&copy; 2025 Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, resetUrl, resetUrl);
    }

    public String buildOfferDeclinedRequeueEmailContent(String studentName, String roomNumber,
                                                          String buildingName, int totalDeclineCount,
                                                          int declinesRemaining, String declineReason,
                                                          String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #ffa500 0%%, #ff6b6b 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #ffa500; padding: 15px; margin: 20px 0; }
                        .warning-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Room Offer Declined</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>We have received your decision to decline the room offer. Your application has been updated accordingly.</p>
                            <div class="info-box">
                                <strong>Declined Room Details:</strong>
                                <ul>
                                    <li><strong>Room:</strong> %s</li>
                                    <li><strong>Building:</strong> %s</li>
                                    <li><strong>Reason:</strong> %s</li>
                                </ul>
                            </div>
                            <div class="warning-box">
                                <strong>⚠️ Decline Status:</strong>
                                <p>You have declined <strong>%d of 2</strong> allowed offers.</p>
                                <p>You have <strong>%d decline(s) remaining</strong>.</p>
                                <p>Your application status has been updated to <strong>AWAITING ALLOCATION</strong> and you have been added back to the waiting list. We will notify you when another room becomes available.</p>
                            </div>
                            <center>
                                <a href="%s" class="button">View Application Status</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>If you have any questions, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, declineReason,
                              totalDeclineCount, declinesRemaining, portalUrl, supportEmail, universityName);
    }

    public String buildOfferDeclinedMaxReachedEmailContent(String studentName, int totalDeclineCount,
                                                             String applicationCode, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Application Closed</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>⛔ Maximum Decline Limit Reached</strong>
                                <p>You have declined <strong>%d room offers</strong>, which is the maximum allowed.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong> and you have been removed from the waiting list.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your current application is now closed</li>
                                    <li>You will not receive any further room offers for this application</li>
                                    <li>You may submit a new application in the next application period</li>
                                </ul>
                            </div>
                            <p>If you have any questions or would like to discuss alternative accommodation options, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, totalDeclineCount, applicationCode, portalUrl, supportEmail, universityName);
    }

    public String buildOfferExpiredEmailContent(String studentName, String applicationCode,
                                                  String roomNumber, String buildingName, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⏰ Room Offer Expired</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>Your room offer has expired</strong>
                                <p>The offer for <strong>Room %s</strong> in <strong>%s</strong> has expired because no response was received within 7 days.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong>.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your current application is now closed</li>
                                    <li>The room has been released for other students</li>
                                    <li>You will need to submit a new application if you still need accommodation</li>
                                </ul>
                            </div>
                            <p>If you still require accommodation, please submit a new application during the next application period.</p>
                            <p>If you have any questions, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, applicationCode, portalUrl, supportEmail, universityName);
    }

    public String buildCheckInExpiredEmailContent(String studentName, String applicationCode,
                                                    String roomNumber, String buildingName, String portalUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 20px auto; background: #fff;
                                    border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                                 color: #fff; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
                        .content { padding: 30px; }
                        .alert-box { background: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0; }
                        .info-box { background: #f8f9fa; border-left: 4px solid #6c757d; padding: 15px; margin: 20px 0; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                 color: #fff; padding: 12px 30px; text-decoration: none;
                                 border-radius: 5px; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center;
                                 font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚫 Check-In Deadline Expired</h1>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <div class="alert-box">
                                <strong>Check-In Deadline Missed</strong>
                                <p>You failed to check in to <strong>Room %s</strong> in <strong>%s</strong> within 7 days of accepting the offer.</p>
                                <p>As a result, your application (<strong>%s</strong>) has been <strong>CANCELLED</strong>.</p>
                            </div>
                            <div class="info-box">
                                <strong>What This Means:</strong>
                                <ul>
                                    <li>Your room allocation has been cancelled</li>
                                    <li>The room has been released for other students</li>
                                    <li>Your current application is now closed</li>
                                    <li>You will need to submit a new application if you still need accommodation</li>
                                </ul>
                            </div>
                            <p>If you still require accommodation, please submit a new application during the next application period.</p>
                            <p>If you have any questions or concerns, please contact our accommodation office.</p>
                            <center>
                                <a href="%s" class="button">View Application</a>
                            </center>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from the Student Accommodation System.</p>
                            <p>For assistance, please contact our support team at %s</p>
                            <p>&copy; 2025 %s Student Accommodation System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(studentName, roomNumber, buildingName, applicationCode, portalUrl, supportEmail, universityName);
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
