package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.allocation.WaitingListService;
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

        @Value("${app.base-url}")
        private String baseUrl;

        @Value("${app.support-email:accommodation@university.edu}")
        private String supportEmail;

        @Value("${app.contact-phone:+27 (0) 11 000 0000}")
        private String contactPhone;

        public void sendApplicationApprovedEmail(Application application, String studentName, String email) {
                String applicationDate = contentBuilder.formatDate(application.getApplicationDate());
                String portalUrl = baseUrl + "/applications/" + application.getId();

                var ctx = new ApplicationApprovedContext(
                                studentName,
                                application.getId(),
                                applicationDate,
                                contentBuilder.getCurrentAcademicYear(),
                                contentBuilder.getCurrentSemester(),
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.APPLICATION_APPROVED, "application-approved", ctx));
                log.info("Application approved email sent to {}", email);
        }

        public void sendApplicationRejectedEmail(Application application, String studentName, String email) {
                String applicationDate = contentBuilder.formatDate(application.getApplicationDate());
                String reviewDate = contentBuilder.formatDate(LocalDateTime.now());
                String rejectionReason = application.getRemarks() != null
                                ? application.getRemarks()
                                : "Your application did not meet the current eligibility criteria.";
                String portalUrl = baseUrl + "/applications/" + application.getId();

                var ctx = new ApplicationRejectedContext(
                                studentName,
                                application.getId(),
                                rejectionReason,
                                applicationDate,
                                reviewDate,
                                contentBuilder.getNextApplicationPeriod(),
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.APPLICATION_REJECTED, "application-rejected", ctx));
                log.info("Application rejected email sent to {}", email);
        }

        public void sendDocumentVerifiedEmail(Document document, String studentName, String email,
                        String documentType) {
                String documentReference = "DOC-" + document.getId();
                String verificationDate = contentBuilder.formatDate(document.getUpdatedAt());
                String portalUrl = baseUrl + "/documents";

                var ctx = new DocumentVerifiedContext(
                                studentName,
                                documentType,
                                documentReference,
                                verificationDate,
                                "Accommodation Office",
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.DOCUMENT_VERIFIED, "document-verified", ctx));
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
                String uploadUrl = baseUrl + "/documents";
                List<String> feedbackPoints = (comment != null && !comment.isEmpty())
                                ? contentBuilder.parseFeedbackPoints(comment)
                                : List.of();

                var ctx = new DocumentRejectedContext(
                                studentName,
                                documentType,
                                documentReference,
                                submissionDate,
                                reviewDate,
                                rejectionReason,
                                resubmissionDeadline,
                                uploadUrl,
                                feedbackPoints);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.DOCUMENT_REJECTED, "document-rejected", ctx));
                log.info("Document rejected email sent to {}", email);
        }

        public void sendDocumentCommentEmail(Document document, String commentText, String studentName, String email,
                        String documentName, String documentType, String subject) {
                // Merged into sendDocumentCommentEmailWithTemplate — delegates to typed context path
                sendDocumentCommentEmailWithTemplate(document, commentText, "Accommodation Office",
                                studentName, email, documentName, documentType);
        }

        public void sendDocumentCommentEmailWithTemplate(Document document, String commentText, String reviewerName,
                        String studentName, String email, String documentName, String documentType) {
                String documentReference = "DOC-" + document.getId();
                String uploadDate = contentBuilder.formatDate(document.getCreatedAt());
                String documentStatus = document.getDocumentStatus() != null ? document.getDocumentStatus().name()
                                : "PENDING_REVIEW";
                String commentDate = contentBuilder.formatDate(LocalDateTime.now());
                String portalUrl = baseUrl + "/documents";

                var ctx = new DocumentCommentContext(
                                studentName,
                                documentType,
                                documentName,
                                documentReference,
                                uploadDate,
                                documentStatus,
                                commentText,
                                reviewerName,
                                commentDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.DOCUMENT_COMMENT, "document-comment", ctx));
                log.info("Document comment email sent to {}", email);
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
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new RoomAllocatedContext(
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
                                portalUrl,
                                contentBuilder.formatDeadline(allocation.getOfferExpiryDate()));

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.ROOM_ALLOCATED, "room-allocated", ctx));
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
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new AllocationProposalContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                floorNumber,
                                roomCapacity,
                                proposalId,
                                score,
                                warnings,
                                moveInDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.ALLOCATION_PROPOSAL, "allocation-proposal", ctx));
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
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new AllocationConfirmedContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                floorNumber,
                                roomType,
                                allocationId,
                                moveInDate,
                                startDate,
                                endDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.ALLOCATION_CONFIRMED, "allocation-confirmed", ctx));
                log.info("Allocation confirmed email sent to {}", email);
        }

        public void sendAllocationRejectedEmail(StagedAllocation stagedAllocation, Room room, Building building,
                        String studentName, String email, String reason, Integer waitingListPosition) {
                String roomNumber = room.getRoomNumber();
                String buildingName = building.getName();
                String proposalId = "PROP-" + stagedAllocation.getId();
                String portalUrl = baseUrl + "/applications";

                var ctx = new AllocationRejectedContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                proposalId,
                                reason,
                                waitingListPosition,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.ALLOCATION_REJECTED, "allocation-rejected", ctx));
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
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new AllocationMovedContext(
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
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.ALLOCATION_MOVED, "allocation-moved", ctx));
                log.info("Allocation moved email sent to {}", email);
        }

        public void sendOfferDeclinedRequeueEmail(RoomAllocation allocation, Room room, String studentName,
                        String email,
                        int totalDeclineCount, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "";
                String declineReason = allocation.getDeclineReason() != null
                                ? allocation.getDeclineReason().getDisplayName()
                                : "Not specified";
                String portalUrl = baseUrl + "/applications";
                // Declines that keep the student on the list, counted the way the decline dialog
                // counts them. The last of MAX_DECLINES closes the application instead.
                int declinesRemaining = Math.max(0, WaitingListService.MAX_DECLINES - 1 - totalDeclineCount);

                var ctx = new OfferDeclinedRequeueContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                totalDeclineCount,
                                declinesRemaining,
                                WaitingListService.MAX_DECLINES,
                                declineReason,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.OFFER_DECLINED_REQUEUE, "offer-declined-requeue", ctx));
                log.info("Offer declined re-queue email sent to {}", email);
        }

        public void sendOfferDeclinedMaxReachedEmail(RoomAllocation allocation, Application application,
                        String studentName, String email, int totalDeclineCount, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String portalUrl = baseUrl + "/applications";

                var ctx = new OfferDeclinedMaxReachedContext(
                                studentName,
                                totalDeclineCount,
                                applicationCode,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.OFFER_DECLINED_MAX_REACHED, "offer-declined-max-reached", ctx));
                log.info("Offer declined max reached email sent to {}", email);
        }

        /**
         * The variables welcome.html reads, as a map that can be persisted alongside the
         * notification row.
         *
         * <p>The welcome path was the only live producer of a notification with null template
         * data, which the retry job could not rebuild an email from, so a failed welcome email was
         * unrecoverable (GH-141). Dispatch goes through this same method, so what is sent and what
         * is stored cannot drift apart.
         */
        public Map<String, Object> welcomeTemplateData(AppUser user, String studentName, String email) {
                Map<String, Object> data = new HashMap<>();
                data.put("templateName", "welcome");
                data.put("studentName", studentName);
                data.put("email", email);
                data.put("studentId", "N/A");
                data.put("registrationDate", contentBuilder.formatDate(user.getCreatedAt()));
                data.put("loginUrl", baseUrl + "/login");
                return data;
        }

        public void sendWelcomeEmail(AppUser user, String studentName, String email) {
                Map<String, Object> data = welcomeTemplateData(user, studentName, email);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.WELCOME, "welcome", data));
                log.info("Welcome email sent to {}", email);
        }

        public void sendPasswordResetEmail(String studentName, String email, String resetToken, String subject) {
                String resetUrl = baseUrl + "/reset-password?token=" + resetToken;

                var ctx = new PasswordResetContext(studentName, resetUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.PASSWORD_RESET, "password-reset", ctx));
                log.info("Password reset email sent to {}", email);
        }

        public void sendOfferExpiredEmail(RoomAllocation allocation, Application application, Room room,
                        String studentName, String email, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String portalUrl = baseUrl + "/applications";

                var ctx = new OfferExpiredContext(
                                studentName,
                                applicationCode,
                                roomNumber,
                                buildingName,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.OFFER_EXPIRED, "offer-expired", ctx));
                log.info("Offer expired email sent to {}", email);
        }

        public void sendCheckInExpiredEmail(RoomAllocation allocation, Application application, Room room,
                        String studentName, String email, String subject) {
                String applicationCode = application != null ? application.getApplicationCode() : "N/A";
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String portalUrl = baseUrl + "/applications";

                var ctx = new CheckInExpiredContext(
                                studentName,
                                applicationCode,
                                roomNumber,
                                buildingName,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.CHECK_IN_EXPIRED, "check-in-expired", ctx));
                log.info("Check-in expired email sent to {}", email);
        }

        public void sendOfferExpiryReminderEmail(RoomAllocation allocation, Room room,
                        String studentName, String email, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String expiryDate = allocation.getOfferExpiryDate() != null
                                ? contentBuilder.formatDate(allocation.getOfferExpiryDate())
                                : "soon";
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new OfferExpiryReminderContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                expiryDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.OFFER_EXPIRY_REMINDER, "offer-expiry-reminder", ctx));
                log.info("Offer expiry reminder email sent to {}", email);
        }

        public void sendCheckInReminderEmail(RoomAllocation allocation, Room room,
                        String studentName, String email, String subject) {
                String roomNumber = room.getRoomNumber();
                String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
                String deadlineDate = allocation.getCheckInExpiryDate() != null
                                ? contentBuilder.formatDate(allocation.getCheckInExpiryDate())
                                : "soon";
                String portalUrl = baseUrl + "/dashboard";

                var ctx = new CheckInReminderContext(
                                studentName,
                                roomNumber,
                                buildingName,
                                deadlineDate,
                                portalUrl);

                monitorEmail(() -> emailService.sendTemplateEmail(email, EmailSubjects.CHECK_IN_REMINDER, "check-in-reminder", ctx));
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
