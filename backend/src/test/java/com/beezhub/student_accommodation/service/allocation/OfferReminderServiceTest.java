package com.beezhub.student_accommodation.service.allocation;

import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.UserNotificationService;
import com.beezhub.student_accommodation.service.notification.EmailDispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferReminderServiceTest {

    @Mock
    private RoomAllocationRepository roomAllocationRepository;

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private EmailDispatchService emailDispatchService;

    @InjectMocks
    private OfferReminderService offerReminderService;

    private RoomAllocation allocation;

    @BeforeEach
    void setUp() {
        AppUser appUser = new AppUser();
        appUser.setId(10L);
        appUser.setEmail("student@test.com");
        appUser.setFirstName("Jane");
        appUser.setLastName("Doe");

        Student student = new Student();
        student.setId(5L);
        student.setAppUser(appUser);

        Building building = new Building();
        building.setName("Res A");

        Room room = new Room();
        room.setRoomNumber("101");
        room.setBuilding(building);

        allocation = new RoomAllocation();
        allocation.setId(1L);
        allocation.setStatus(AllocationStatus.OFFERED);
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setOfferExpiryDate(LocalDateTime.now().plusHours(12));
        allocation.setReminder24hSent(false);
    }

    @Test
    void reminderJob_withOfferExpiring24hFromNow_sendsReminder() {
        when(roomAllocationRepository.findOffersExpiringSoonWithoutReminder(any(), any()))
                .thenReturn(List.of(allocation));
        when(roomAllocationRepository.save(any())).thenReturn(allocation);

        int result = offerReminderService.sendOfferExpiryReminders();

        assertThat(result).isEqualTo(1);
        verify(userNotificationService).createOfferExpiryReminderNotification(
                eq(10L), eq("101"), eq("Res A"));
        verify(emailDispatchService).sendOfferExpiryReminderEmail(
                eq(allocation), any(Room.class), eq("Jane Doe"),
                eq("student@test.com"), anyString());

        ArgumentCaptor<RoomAllocation> captor = ArgumentCaptor.forClass(RoomAllocation.class);
        verify(roomAllocationRepository).save(captor.capture());
        assertThat(captor.getValue().isReminder24hSent()).isTrue();
    }

    @Test
    void reminderJob_withReminderAlreadySent_doesNotSendAgain() {
        // Repository returns empty because flag is already true — the query filters it out
        when(roomAllocationRepository.findOffersExpiringSoonWithoutReminder(any(), any()))
                .thenReturn(Collections.emptyList());

        int result = offerReminderService.sendOfferExpiryReminders();

        assertThat(result).isEqualTo(0);
        verifyNoInteractions(userNotificationService);
        verifyNoInteractions(emailDispatchService);
        verify(roomAllocationRepository, never()).save(any());
    }

    @Test
    void reminderJob_withNoExpiringOffers_returnsZero() {
        when(roomAllocationRepository.findOffersExpiringSoonWithoutReminder(any(), any()))
                .thenReturn(Collections.emptyList());

        int result = offerReminderService.sendOfferExpiryReminders();

        assertThat(result).isEqualTo(0);
        verifyNoInteractions(userNotificationService);
        verifyNoInteractions(emailDispatchService);
    }

    @Test
    void reminderJob_whenEmailThrows_continuesAndReturnsSuccessCount() {
        RoomAllocation second = buildAllocation(2L, 20L, "200", "Res B");
        when(roomAllocationRepository.findOffersExpiringSoonWithoutReminder(any(), any()))
                .thenReturn(List.of(allocation, second));
        when(roomAllocationRepository.save(any())).thenReturn(allocation);

        // First allocation's email throws; second succeeds
        doThrow(new RuntimeException("SMTP failure"))
                .when(emailDispatchService)
                .sendOfferExpiryReminderEmail(eq(allocation), any(), any(), any(), any());

        int result = offerReminderService.sendOfferExpiryReminders();

        assertThat(result).isEqualTo(1);
        verify(roomAllocationRepository, times(1)).save(any());
    }

    // ---- helpers ----

    private RoomAllocation buildAllocation(Long id, Long userId, String roomNumber, String buildingName) {
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail("user" + userId + "@test.com");
        appUser.setFirstName("User");
        appUser.setLastName(String.valueOf(userId));

        Student student = new Student();
        student.setId(userId);
        student.setAppUser(appUser);

        Building building = new Building();
        building.setName(buildingName);

        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setBuilding(building);

        RoomAllocation ra = new RoomAllocation();
        ra.setId(id);
        ra.setStatus(AllocationStatus.OFFERED);
        ra.setStudent(student);
        ra.setRoom(room);
        ra.setOfferExpiryDate(LocalDateTime.now().plusHours(12));
        ra.setReminder24hSent(false);
        return ra;
    }
}
