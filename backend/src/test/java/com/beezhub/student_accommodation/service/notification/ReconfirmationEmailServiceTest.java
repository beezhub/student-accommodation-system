package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.service.EmailService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReconfirmationEmailServiceTest {

    private ReconfirmationEmailService service;
    private RoomAllocation allocation;

    @BeforeEach
    void setUp() {
        service = new ReconfirmationEmailService(mock(EmailService.class), new EmailContentBuilder(),
                mock(MeterRegistry.class));
        ReflectionTestUtils.setField(service, "baseUrl", "http://test-url");

        Building building = new Building();
        building.setName("West Residence");
        Room room = new Room();
        room.setRoomNumber("12");
        room.setBuilding(building);
        allocation = new RoomAllocation();
        allocation.setRoom(room);
        allocation.setStayStartDate(LocalDate.of(2027, 2, 1));
    }

    /**
     * GH-531: the release job frees the place on the release date itself, at 08:00 South African
     * time, so "confirm by the release date" was usually too late. The email names the last full
     * day the student can confirm.
     */
    @Test
    void request_confirmByIsTheDayBeforeTheRelease() {
        ReconfirmationEmailService.Email email = service.request(allocation, false, LocalDate.of(2027, 1, 18), "Sam");

        ReconfirmRequestContext ctx = (ReconfirmRequestContext) email.context();
        assertThat(ctx.confirmByDate()).isEqualTo("17 January 2027");
    }

    @Test
    void reminder_confirmByIsTheDayBeforeTheRelease() {
        ReconfirmationEmailService.Email email = service.request(allocation, true, LocalDate.of(2027, 1, 18), "Sam");

        ReconfirmReminderContext ctx = (ReconfirmReminderContext) email.context();
        assertThat(ctx.confirmByDate()).isEqualTo("17 January 2027");
    }
}
