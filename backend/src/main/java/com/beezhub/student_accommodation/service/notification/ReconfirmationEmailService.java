package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Builds and sends the reconfirmation emails (GH-496): the "Still coming?" request, its reminders
 * and the release notice. Kept apart from {@link EmailDispatchService}, which is over the service
 * size limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconfirmationEmailService {

    static final String REQUEST_TEMPLATE = "reconfirm-request";
    static final String REMINDER_TEMPLATE = "reconfirm-reminder";
    static final String RELEASED_TEMPLATE = "place-released";

    private final EmailService emailService;
    private final EmailContentBuilder contentBuilder;
    private final MeterRegistry meterRegistry;

    @Value("${app.base-url}")
    private String baseUrl;

    /** The request or a reminder: the template name, the subject and the context. */
    public Email request(RoomAllocation allocation, boolean reminder, LocalDate releaseDate, String studentName) {
        Room room = allocation.getRoom();
        String moveIn = contentBuilder.formatDate(allocation.getStayStartDate());
        String release = contentBuilder.formatDate(releaseDate);
        String portalUrl = baseUrl + "/dashboard";
        if (reminder) {
            return new Email(REMINDER_TEMPLATE, EmailSubjects.RECONFIRM_REMINDER, new ReconfirmReminderContext(
                    studentName, room.getRoomNumber(), buildingName(room), moveIn, release, portalUrl));
        }
        return new Email(REQUEST_TEMPLATE, EmailSubjects.RECONFIRM_REQUEST, new ReconfirmRequestContext(
                studentName, room.getRoomNumber(), buildingName(room), moveIn, release, portalUrl));
    }

    /** The notice that the place was released. */
    public Email released(RoomAllocation allocation, String studentName) {
        Room room = allocation.getRoom();
        Application application = allocation.getApplication();
        return new Email(RELEASED_TEMPLATE, EmailSubjects.PLACE_RELEASED, new PlaceReleasedContext(
                studentName,
                application != null ? application.getApplicationCode() : "N/A",
                room.getRoomNumber(),
                buildingName(room),
                contentBuilder.formatDate(allocation.getStayStartDate()),
                baseUrl + "/applications"));
    }

    public void send(String to, Email email) {
        try {
            emailService.sendTemplateEmail(to, email.subject(), email.templateName(), email.context());
            meterRegistry.counter("sas.emails.sent").increment();
        } catch (RuntimeException e) {
            meterRegistry.counter("sas.emails.failed").increment();
            throw e;
        }
        log.info("action=reconfirmationEmailSent template={}", email.templateName());
    }

    private static String buildingName(Room room) {
        return room.getBuilding() != null ? room.getBuilding().getName() : "Unknown Building";
    }

    /** One email: its template, subject and typed variables. */
    public record Email(String templateName, String subject, EmailContext context) {

        /** The variables with the template name, as stored on the notification for retries. */
        public Map<String, Object> templateData() {
            Map<String, Object> data = EmailService.toVariableMap(context);
            data.put("templateName", templateName);
            return data;
        }
    }
}
