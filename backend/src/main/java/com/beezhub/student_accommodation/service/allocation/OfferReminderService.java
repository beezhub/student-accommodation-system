package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends 24-hour offer-expiry reminder notifications (both email and in-app).
 *
 * <p>The job queries OFFERED allocations whose {@code offerExpiryDate} falls within
 * the next 24 hours and whose {@code reminder24hSent} flag is {@code false}.
 * After a successful send it flips the flag so re-runs are idempotent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferReminderService {

    private static final String EMAIL_SUBJECT = "Action Required: Your Room Offer Expires in 24 Hours";
    private static final int REMINDER_WINDOW_HOURS = 24;

    private final RoomAllocationRepository roomAllocationRepository;
    private final UserNotificationService userNotificationService;
    private final EmailDispatchService emailDispatchService;

    /**
     * Finds all OFFERED allocations expiring within the next 24 hours that have
     * not yet received a reminder, dispatches both channels, and marks each one sent.
     *
     * @return number of reminders successfully sent
     */
    @Transactional
    public int sendOfferExpiryReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(REMINDER_WINDOW_HOURS);

        List<RoomAllocation> candidates =
                roomAllocationRepository.findOffersExpiringSoonWithoutReminder(now, windowEnd);

        if (candidates.isEmpty()) {
            log.info("offerReminder: no candidates found in window [{}, {}]", now, windowEnd);
            return 0;
        }

        log.info("offerReminder: found {} candidate(s) expiring before {}", candidates.size(), windowEnd);
        int successCount = 0;

        for (RoomAllocation allocation : candidates) {
            try {
                sendReminder(allocation);
                allocation.setReminder24hSent(true);
                roomAllocationRepository.save(allocation);
                successCount++;
            } catch (Exception e) {
                log.error("offerReminder: failed for allocationId={} studentId={}: {}",
                        allocation.getId(),
                        allocation.getStudent() != null ? allocation.getStudent().getId() : "null",
                        e.getMessage(), e);
            }
        }

        log.info("offerReminder: completed sent={} failed={}", successCount,
                candidates.size() - successCount);
        return successCount;
    }

    private void sendReminder(RoomAllocation allocation) {
        Room room = allocation.getRoom();
        String roomNumber = room.getRoomNumber();
        String buildingName = room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
        AppUser appUser = allocation.getStudent().getAppUser();
        Long userId = appUser.getId();
        String email = appUser.getEmail();
        String studentName = appUser.getFirstName() + " " + appUser.getLastName();

        userNotificationService.createOfferExpiryReminderNotification(userId, roomNumber, buildingName);
        emailDispatchService.sendOfferExpiryReminderEmail(allocation, room, studentName, email, EMAIL_SUBJECT);

        log.info("offerReminder: sent allocationId={} userId={} email={}",
                allocation.getId(), userId, email);
    }
}
