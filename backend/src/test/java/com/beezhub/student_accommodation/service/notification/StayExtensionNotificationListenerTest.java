package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.StayExtendedEvent;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StayExtensionNotificationListenerTest {

    @Mock NotificationPersistenceService notificationService;
    @Mock EmailService emailService;
    @Mock UserNotificationService userNotificationService;
    @Mock AppUserRepository appUserRepository;
    @Mock EntityManager entityManager;

    private StayExtensionNotificationListener listener;
    private RoomAllocation allocation;
    private final Notification stored = Notification.builder().id(1L).build();

    @BeforeEach
    void setUp() {
        listener = new StayExtensionNotificationListener(notificationService, emailService, new EmailContentBuilder(),
                userNotificationService, appUserRepository, entityManager, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(listener, "baseUrl", "http://test-url");
        Student student = new Student();
        student.setAppUser(AppUser.builder().id(1L).email("student@example.com").firstName("Thandi").lastName("M").build());
        Building building = new Building();
        building.setName("West");
        Room room = new Room();
        room.setRoomNumber("12");
        room.setBuilding(building);
        allocation = new RoomAllocation();
        allocation.setId(40L);
        allocation.setTenantId(7L);
        allocation.setStudent(student);
        allocation.setRoom(room);
    }

    private StayExtendedEvent event() {
        return new StayExtendedEvent(40L, "Semester 2", LocalDate.of(2027, 6, 30), LocalDate.of(2027, 11, 30));
    }

    @Test
    void handleStayExtended_confirmsToTheStudentAndTellsTheCompanysAdmins() {
        when(entityManager.find(RoomAllocation.class, 40L)).thenReturn(allocation);
        when(notificationService.createNotification(any(), any(), any(NotificationType.class), any(), any(), any(), any()))
                .thenReturn(stored);
        when(appUserRepository.findByTenantIdAndUserRole(7L, UserRole.TENANT_ADMIN))
                .thenReturn(List.of(AppUser.builder().id(9L).build()));

        listener.handleStayExtended(event());

        verify(emailService).sendTemplateEmail(eq("student@example.com"), eq(EmailSubjects.STAY_EXTENDED),
                eq(StayExtensionNotificationListener.TEMPLATE), eq(new StayExtendedContext("Thandi M", "12", "West",
                        "Semester 2", "30 November 2027", "http://test-url/dashboard")));
        verify(notificationService).markNotificationAsSent(stored);
        verify(userNotificationService).createNotification(9L, StayExtensionNotificationListener.COMPANY_NOTIFICATION_TYPE,
                "Stay extended", "Thandi M is staying on in room 12 for Semester 2, until 30 November 2027.",
                Map.of("allocationId", 40L));
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void handleStayExtended_emailFails_isLeftForRetryAndTheCompanyIsStillTold() {
        when(entityManager.find(RoomAllocation.class, 40L)).thenReturn(allocation);
        when(notificationService.createNotification(any(), any(), any(NotificationType.class), any(), any(), any(), any()))
                .thenReturn(stored);
        IllegalStateException down = new IllegalStateException("smtp down");
        doThrow(down).when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), any(EmailContext.class));

        listener.handleStayExtended(event());

        verify(notificationService).handleNotificationFailure(stored, down);
        verify(appUserRepository).findByTenantIdAndUserRole(7L, UserRole.TENANT_ADMIN);
    }

    @Test
    void handleStayExtended_allocationGone_sendsNothing() {
        listener.handleStayExtended(event());

        verifyNoInteractions(emailService, notificationService, userNotificationService);
    }
}
