package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDispatchService {

        private final EmailService emailService;
        private final EmailContentBuilder contentBuilder;
        private final MeterRegistry meterRegistry;

        @Value("${app.base-url:http://localhost:3000}")
        private String baseUrl;

        @Value("${app.support-email:accommodation@university.edu}")
        private String supportEmail;

        public void sendApplicationApprovedEmail(Application application, String studentName, String email) {
                String applicationDate = contentBuilder.formatDate(application.getApplicationDate());
                String portalUrl = baseUrl + "/portal/applications/" + application.getId();

                monitorEmail(() -> emailService.sendApplicationApprovedEmail(
                                email,
                                studentName,
                                application.getId(),
                                applicationDate,
                                contentBuilder.getCurrentAcademicYear(),
                                contentBuilder.getCurrentSemester(),
                                portalUrl));
                log.info("Application approved email sent to {}", email);
        }

        public void sendApplicationRejectedEmail(Application application, String studentName, String email) {
                String applicationDate = contentBuilder.formatDate(application.getApplicationDate());
                String reviewDate = contentBuilder.formatDate(LocalDateTime.now());
                String rejectionReason = application.getRemarks() != null
                                ? application.getRemarks()
                                : "Your application did not meet the current eligibility criteria.";
                String portalUrl = baseUrl + "/portal/applications/" + application.getId();

                monitorEmail(() -> emailService.sendApplicationRejectedEmail(
                                email,
                                studentName,
                                application.getId(),
                                rejectionReason,
                                applicationDate,
                                reviewDate,
                                contentBuilder.getNextApplicationPeriod(),
                                portalUrl));
                log.info("Application rejected email sent to {}", email);
        }

        public void sendDocumentVerifiedEmail(Document document, String studentName, String email,
                        String documentType) {
                String documentReference = "DOC-" + document.getId();
                String verificationDate = contentBuilder.formatDate(document.getUpdatedAt());
                String portalUrl = baseUrl + "/portal/documents";

                monitorEmail(() -> emailService.sendDocumentVerifiedEmail(
                                email,
                                studentName,
                                documentType,
                                documentReference,
                                verificationDate,
                                "Accommodation Office",
                                portalUrl));
                log.info("Document verified email sent to {}", email);
        }

        public void sendDocumentRejectedEmail(Document document, String studentName, String email, String documentType,
                        String comment) {
                String documentReference = "DOC-" + document.getId();
                String submissionDate = contentBuilder.formatDate(document.getCreatedAt());
                String reviewDate = contentBuilder.formatDate(document.getUpdatedAt());
                String rejectionReason = comment != null && !comment.isEmpty()
                                ? comment
                                : "The document does not meet our verification requirements.";
                String resubmissionDeadline = contentBuilder.formatDate(LocalDateTime.now().plusDays(7));
                String uploadUrl = baseUrl + "/portal/documents/upload";

                Map<String, Object> additionalVariables = new HashMap<>();
                if (comment != null && !comment.isEmpty()) {
                        List<String> feedbackPoints = contentBuilder.parseFeedbackPoints(comment);
                        if (!feedbackPoints.isEmpty()) {
                                additionalVariables.put("feedbackPoints", feedbackPoints);
                        }
                }

                monitorEmail(() -> emailService.sendDocumentRejectedEmail(
                                email,
                                studentName,
                                documentType,
                                documentReference,
                                submissionDate,
                                reviewDate,
                                rejectionReason,
                                resubmissionDeadline,
                                uploadUrl,
                                additionalVariables));
                log.info("Document rejected email sent to {}", email);
        }

        public void sendDocumentCommentEmail(Document document, String commentText, String studentName, String email,
                        String documentName, String documentType, String subject) {
                String documentReference = "DOC-" + document.getId();
                String portalUrl = baseUrl + "/portal/documents";

                String htmlContent = contentBuilder.buildDocumentCommentEmailContent(
                                studentName,
                                documentName,
                                documentType,
                                documentReference,
                                commentText,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Document comment email sent to {}", email);
        }

        public void sendDocumentCommentEmailWithTemplate(Document document, String commentText, String reviewerName,
                        String studentName, String email, String documentName, String documentType) {
                String applicationId = "N/A";
                String uploadDate = contentBuilder.formatDate(document.getCreatedAt());
                String documentStatus = document.getDocumentStatus() != null ? document.getDocumentStatus().name()
                                : "PENDING_REVIEW";
                String commentDate = contentBuilder.formatDate(LocalDateTime.now());
                String portalUrl = baseUrl + "/portal/documents";

                monitorEmail(() -> emailService.sendDocumentCommentEmail(
                                email,
                                studentName,
                                documentType,
                                documentName,
                                applicationId,
                                uploadDate,
                                documentStatus,
                                commentText,
                                reviewerName,
                                commentDate,
                                portalUrl,
                                supportEmail,
                                null));
                log.info("Document comment email (template) sent to {}", email);
        }

        public void sendRoomAllocatedEmail(RoomAllocation allocation, Room room, Building building,
                        String studentName, String email) {
                String roomNumber = room.getRoomNumber();
                String buildingName = building.getName();
                String buildingAddress = building.getAddress() != null ? building.getAddress() : "";
                Integer floorNumber = contentBuilder.extractFloorNumber(roomNumber);
                String roomType = contentBuilder.determineRoomType(room);
                String bedNumber = "1";
                String allocationId = "ALLOC-" + allocation.getId();
                String startDate = contentBuilder.formatDate(allocation.getAllocationDate());
                String endDate = allocation.getEndDate() != null
                                ? contentBuilder.formatDate(allocation.getEndDate())
                                : contentBuilder.formatDate(allocation.getAllocationDate().plusMonths(10));
                String duration = contentBuilder.calculateDuration(allocation.getAllocationDate(),
                                allocation.getEndDate());
                String officeLocation = "Main Administration Building, Ground Floor";
                String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

                monitorEmail(() -> emailService.sendRoomAllocatedEmail(
                                email,
                                studentName,
                                roomNumber,
                                buildingName,
                                floorNumber,
                                roomType,
                                bedNumber,
                                allocationId,
                                startDate,
                                endDate,
                                duration,
                                officeLocation,
                                buildingAddress,
                                portalUrl));
                log.info("Room allocation email sent to {}", email);
        }

        public void sendAllocationProposalEmail(StagedAllocation stagedAllocation, Room room, Building building,
                        String studentName, String email) {
                String roomNumber = room.getRoomNumber();
                String buildingName = building.getName();
                Integer floorNumber = contentBuilder.extractFloorNumber(roomNumber);
                Integer roomCapacity = room.getCapacity();
                String proposalId = "PROP-" + stagedAllocation.getId();
                BigDecimal score = stagedAllocation.getScore();
                List<String> warnings = contentBuilder.parseWarnings(stagedAllocation.getWarnings());
                String moveInDate = null;
                String portalUrl = baseUrl + "/portal/allocations/proposal/" + stagedAllocation.getId();

                monitorEmail(() -> emailService.sendAllocationProposalEmail(
                                email,
                                studentName,
                                roomNumber,
                                buildingName,
                                floorNumber,
                                roomCapacity,
                                proposalId,
                                score,
                                warnings,
                                moveInDate,
                                portalUrl,
                                supportEmail));
                log.info("Allocation proposal email sent to {}", email);
        }

        public void sendAllocationConfirmedEmail(RoomAllocation allocation, Room room, Building building,
                        String studentName, String email) {
                String roomNumber = room.getRoomNumber();
                String buildingName = building.getName();
                Integer floorNumber = contentBuilder.extractFloorNumber(roomNumber);
                String roomType = contentBuilder.determineRoomType(room);
                String allocationId = "ALLOC-" + allocation.getId();
                String moveInDate = contentBuilder.formatDate(allocation.getAllocationDate());
                String startDate = contentBuilder.formatDate(allocation.getAllocationDate());
                String endDate = allocation.getEndDate() != null
                                ? contentBuilder.formatDate(allocation.getEndDate())
                                : contentBuilder.formatDate(allocation.getAllocationDate().plusMonths(10));
                String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

                monitorEmail(() -> emailService.sendAllocationConfirmedEmail(
                                email,
                                studentName,
                                roomNumber,
                                buildingName,
                                floorNumber,
                                roomType,
                                allocationId,
                                moveInDate,
                                startDate,
                                endDate,
                                portalUrl,
                                supportEmail,
                                null));
                log.info("Allocation confirmed email sent to {}", email);
        }

        public void sendAllocationRejectedEmail(StagedAllocation stagedAllocation, Room room, Building building,
                        String studentName, String email, String reason, Integer waitingListPosition) {
                String roomNumber = room.getRoomNumber();
                String buildingName = building.getName();
                String proposalId = "PROP-" + stagedAllocation.getId();
                String portalUrl = baseUrl + "/portal/applications";

                monitorEmail(() -> emailService.sendAllocationRejectedEmail(
                                email,
                                studentName,
                                roomNumber,
                                buildingName,
                                proposalId,
                                reason,
                                waitingListPosition,
                                portalUrl,
                                supportEmail,
                                null));
                log.info("Allocation rejected email sent to {}", email);
        }

        public void sendAllocationMovedEmail(RoomAllocation newAllocation, Room oldRoom, Building newBuilding,
                        Building oldBuilding,
                        String studentName, String email, LocalDateTime effectiveDate, String reason) {
                String oldRoomNumber = oldRoom.getRoomNumber();
                String oldBuildingName = oldBuilding.getName();
                Integer oldFloorNumber = contentBuilder.extractFloorNumber(oldRoomNumber);
                Room newRoom = newAllocation.getRoom();
                String newRoomNumber = newRoom.getRoomNumber();
                String newBuildingName = newBuilding.getName();
                Integer newFloorNumber = contentBuilder.extractFloorNumber(newRoomNumber);
                String newRoomType = contentBuilder.determineRoomType(newRoom);
                String effectiveDateStr = contentBuilder.formatDate(effectiveDate);
                String portalUrl = baseUrl + "/portal/allocations/" + newAllocation.getId();

                monitorEmail(() -> emailService.sendAllocationMovedEmail(
                                email,
                                studentName,
                                oldRoomNumber,
                                oldBuildingName,
                                oldFloorNumber,
                                newRoomNumber,
                                newBuildingName,
                                newFloorNumber,
                                newRoomType,
                                effectiveDateStr,
                                reason,
                                portalUrl,
                                supportEmail,
                                null));
                log.info("Allocation moved email sent to {}", email);
        }

        public void sendOfferDeclinedRequeueEmail(RoomAllocation allocation, Room room, String studentName,
                        String email,
                        int totalDeclineCount, int maxDeclineCount, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "";
                int declinesRemaining = maxDeclineCount - totalDeclineCount;
                String declineReason = allocation.getDeclineReason() != null
                                ? allocation.getDeclineReason().getDisplayName()
                                : "Not specified";
                String portalUrl = baseUrl + "/portal/applications";

                String htmlContent = contentBuilder.buildOfferDeclinedRequeueEmailContent(
                                studentName,
                                roomNumber,
                                buildingName,
                                totalDeclineCount,
                                declinesRemaining,
                                declineReason,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Offer declined re-queue email sent to {}", email);
        }

        public void sendOfferDeclinedMaxReachedEmail(RoomAllocation allocation, Application application,
                        String studentName, String email, int totalDeclineCount, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String portalUrl = baseUrl + "/portal/applications";

                String htmlContent = contentBuilder.buildOfferDeclinedMaxReachedEmailContent(
                                studentName,
                                totalDeclineCount,
                                applicationCode,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Offer declined max reached email sent to {}", email);
        }

        public void sendWelcomeEmail(AppUser user, String studentName, String email) {
                String studentId = "N/A";
                String registrationDate = contentBuilder.formatDate(user.getCreatedAt());
                String loginUrl = baseUrl + "/login";
                String guideUrl = baseUrl + "/guide";

                monitorEmail(() -> emailService.sendWelcomeEmail(
                                email,
                                studentName,
                                email,
                                studentId,
                                registrationDate,
                                loginUrl,
                                guideUrl));
                log.info("Welcome email sent to {}", email);
        }

        public void sendPasswordResetEmail(String studentName, String email, String resetToken, String subject) {
                String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
                String htmlContent = contentBuilder.buildPasswordResetEmailContent(studentName, resetUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Password reset email sent to {}", email);
        }

        public void sendOfferExpiredEmail(RoomAllocation allocation, Application application, Room room,
                        String studentName, String email, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String portalUrl = baseUrl + "/portal/applications";

                String htmlContent = contentBuilder.buildOfferExpiredEmailContent(
                                studentName,
                                applicationCode,
                                roomNumber,
                                buildingName,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Offer expired email sent to {}", email);
        }

        public void sendCheckInExpiredEmail(RoomAllocation allocation, Application application, Room room,
                        String studentName, String email, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String portalUrl = baseUrl + "/portal/applications";

                String htmlContent = contentBuilder.buildCheckInExpiredEmailContent(
                                studentName,
                                applicationCode,
                                roomNumber,
                                buildingName,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Check-in expired email sent to {}", email);
        }

        public void sendOfferExpiryReminderEmail(RoomAllocation allocation, Room room,
                        String studentName, String email, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String expiryDate = allocation.getOfferExpiryDate() != null
                                ? contentBuilder.formatDate(allocation.getOfferExpiryDate())
                                : "soon";
                String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

                String htmlContent = contentBuilder.buildOfferExpiryReminderEmailContent(
                                studentName,
                                roomNumber,
                                buildingName,
                                expiryDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Offer expiry reminder email sent to {}", email);
        public void sendCheckInReminderEmail(RoomAllocation allocation, Room room,
                        String studentName, String email, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String deadlineDate = allocation.getCheckInExpiryDate() != null
                                ? contentBuilder.formatDate(allocation.getCheckInExpiryDate())
                                : "soon";
                String portalUrl = baseUrl + "/portal/allocations/" + allocation.getId();

                String htmlContent = contentBuilder.buildCheckInReminderEmailContent(
                                studentName,
                                roomNumber,
                                buildingName,
                                deadlineDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendHtmlEmail(email, subject, htmlContent));
                log.info("Check-in reminder email sent to {}", email);
        }

        private void monitorEmail(Runnable action) {
                try {
                        action.run();
                        meterRegistry.counter("sas.emails.sent").increment();
                } catch (Exception e) {
                        meterRegistry.counter("sas.emails.failed").increment();
                        throw e;
                }
        }
}
