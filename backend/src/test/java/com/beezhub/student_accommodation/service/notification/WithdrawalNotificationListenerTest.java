package com.beezhub.student_accommodation.service.notification;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Notification;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.NotificationType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent;
import com.beezhub.student_accommodation.model.event.ApplicationsWithdrawnEvent.Withdrawn;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.service.EmailService;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GH-497: one email to the student, and each affected company told about its own applications. */
class WithdrawalNotificationListenerTest {

    private NotificationPersistenceService persistence;
    private EmailService emailService;
    private UserNotificationService userNotificationService;
    private AppUserRepository appUserRepository;
    private EntityManager entityManager;
    private SimpleMeterRegistry meterRegistry;
    private WithdrawalNotificationListener listener;
    private final Notification notification = Notification.builder().id(1L).build();
    private final List<Long> tenantsSeen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        persistence = mock(NotificationPersistenceService.class);
        emailService = mock(EmailService.class);
        userNotificationService = mock(UserNotificationService.class);
        appUserRepository = mock(AppUserRepository.class);
        entityManager = mock(EntityManager.class);
        meterRegistry = new SimpleMeterRegistry();
        listener = new WithdrawalNotificationListener(persistence, emailService, userNotificationService,
                appUserRepository, entityManager, meterRegistry);
        ReflectionTestUtils.setField(listener, "baseUrl", "http://portal");

        AppUser user = AppUser.builder().id(9L).email("jane@example.com").firstName("Jane").lastName("Doe").build();
        Student student = new Student();
        student.setId(1L);
        student.setAppUser(user);
        when(entityManager.find(Student.class, 1L)).thenReturn(student);
        Building building = new Building();
        building.setName("West Residence");
        Room room = new Room();
        room.setRoomNumber("12");
        room.setBuilding(building);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setRoom(room);
        when(entityManager.find(RoomAllocation.class, 50L)).thenReturn(allocation);
        when(persistence.createNotification(any(), any(), any(NotificationType.class), any(), any(), any(), any()))
                .thenReturn(notification);
        when(appUserRepository.findByTenantIdAndUserRole(anyLong(), eq(UserRole.TENANT_ADMIN))).thenAnswer(i -> {
            tenantsSeen.add(TenantContext.getTenantId());
            Long tenantId = i.getArgument(0);
            return List.of(AppUser.builder().id(100 + tenantId).build());
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static ApplicationsWithdrawnEvent event() {
        return new ApplicationsWithdrawnEvent(1L, 50L, List.of(
                new Withdrawn(11L, "APP-11", 8L, "Company B", "Semester 1, 2027", 0),
                new Withdrawn(12L, "APP-12", 9L, "Company C", "Annual, 2027", 1),
                new Withdrawn(13L, "APP-13", 9L, null, null, 0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleApplicationsWithdrawn_oneEmailListsEveryWithdrawnApplication() {
        listener.handleApplicationsWithdrawn(event());

        ArgumentCaptor<Map<String, Object>> stored = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createNotification(eq("jane@example.com"), eq(EmailSubjects.APPLICATIONS_WITHDRAWN),
                eq(NotificationType.GENERAL), any(), eq(50L), eq("RoomAllocation"), stored.capture());
        assertThat(stored.getValue()).containsEntry("templateName", "applications-withdrawn")
                .containsEntry("acceptedPlace", "Room 12, West Residence")
                .containsEntry("portalUrl", "http://portal/applications");
        assertThat((List<String>) stored.getValue().get("withdrawn")).containsExactly(
                "APP-11 at Company B, Semester 1, 2027", "APP-12 at Company C, Annual, 2027", "APP-13");
        verify(emailService, times(1)).sendTemplateEmail(eq("jane@example.com"),
                eq(EmailSubjects.APPLICATIONS_WITHDRAWN), eq("applications-withdrawn"), any(EmailContext.class));
        verify(persistence).markNotificationAsSent(notification);
        assertThat(meterRegistry.counter("sas.emails.sent").count()).isEqualTo(1.0);
    }

    @Test
    void handleApplicationsWithdrawn_eachCompanyToldAboutItsOwnApplicationsOnly() {
        listener.handleApplicationsWithdrawn(event());

        assertThat(tenantsSeen).containsExactly(8L, 9L);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(userNotificationService).createNotification(eq(108L), eq("APPLICATION_WITHDRAWN"),
                eq("Application withdrawn"), message.capture(), eq(Map.of("applicationId", 11L)));
        assertThat(message.getValue()).contains("APP-11").contains("Semester 1, 2027")
                .doesNotContain("Company").doesNotContain("West Residence");
        verify(userNotificationService).createNotification(eq(109L), any(), any(),
                org.mockito.ArgumentMatchers.contains("bed released"), eq(Map.of("applicationId", 12L)));
        verify(userNotificationService).createNotification(eq(109L), any(), any(), any(),
                eq(Map.of("applicationId", 13L)));
        verify(userNotificationService, never()).createNotification(eq(108L), any(), any(), any(),
                eq(Map.of("applicationId", 12L)));
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void handleApplicationsWithdrawn_sendFails_recordedForRetry() {
        RuntimeException failure = new RuntimeException("smtp");
        doThrow(failure).when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), any(EmailContext.class));

        listener.handleApplicationsWithdrawn(event());

        verify(persistence).handleNotificationFailure(notification, failure);
        assertThat(meterRegistry.counter("sas.emails.failed").count()).isEqualTo(1.0);
        verify(userNotificationService, times(3)).createNotification(anyLong(), any(), any(), any(), anyMap());
    }

    @Test
    void handleApplicationsWithdrawn_companyNotificationFails_otherCompaniesStillTold() {
        doAnswer(i -> {
            throw new IllegalStateException("sse");
        }).when(userNotificationService).createNotification(eq(108L), any(), any(), any(), anyMap());

        listener.handleApplicationsWithdrawn(event());

        verify(userNotificationService, times(2)).createNotification(eq(109L), any(), any(), any(), anyMap());
    }

    @Test
    void handleApplicationsWithdrawn_studentMissing_companiesStillTold() {
        when(entityManager.find(Student.class, 1L)).thenReturn(null);
        when(entityManager.find(RoomAllocation.class, 50L)).thenReturn(null);

        listener.handleApplicationsWithdrawn(event());

        verify(persistence, never()).createNotification(any(), any(), any(NotificationType.class), any(), any(), any(), any());
        verify(userNotificationService, times(3)).createNotification(anyLong(), any(), any(), any(), anyMap());
    }
}
