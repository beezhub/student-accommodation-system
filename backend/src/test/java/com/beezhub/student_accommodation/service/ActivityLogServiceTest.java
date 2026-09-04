package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ActivityLogPageResponse;
import com.beezhub.student_accommodation.model.entity.ActivityLog;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ActivityLogRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ActivityLogService activityLogService;

    private AppUser testUser;
    private ActivityLog activityLog;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .userRole(UserRole.PLATFORM_ADMIN)
                .build();

        activityLog = ActivityLog.builder()
                .id(100L)
                .user(testUser)
                .action("CREATE")
                .entityType("BUILDING")
                .entityId(1L)
                .description("Created a building")
                .ipAddress("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .metadata("{\"key\":\"value\"}")
                .build();
    }

    @Test
    void getActivityLogs_ValidFilters_ReturnsPage() throws Exception {
        Page<ActivityLog> page = new PageImpl<>(List.of(activityLog));
        when(activityLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(objectMapper.readValue(eq("{\"key\":\"value\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "value"));

        ActivityLogPageResponse response = activityLogService.getActivityLogs(
                1L, "PLATFORM_ADMIN", "CREATE", "BUILDING", 
                LocalDate.now().minusDays(1), LocalDate.now(), 0, 10);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getMetadata()).containsEntry("key", "value");
    }

    @Test
    void getActivityLogs_InvalidDates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
                activityLogService.getActivityLogs(null, null, null, null, 
                        LocalDate.now(), LocalDate.now().minusDays(1), 0, 10));
    }

    @Test
    void exportActivityLogsToCsv_ValidData_ReturnsCsvBytes() {
        when(activityLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(activityLog));

        byte[] csvBytes = activityLogService.exportActivityLogsToCsv(null, null, null, null, null, null);

        assertThat(csvBytes).isNotEmpty();
        String csvContent = new String(csvBytes);
        assertThat(csvContent).contains("ID,Timestamp,User Email,User Name,User Role,Action,Entity Type,Entity ID,Description,IP Address");
        assertThat(csvContent).contains("test@example.com");
        assertThat(csvContent).contains("CREATE");
    }
    
    @Test
    void exportActivityLogsToCsv_NullUserFields_HandlesGracefully() {
        ActivityLog logNoUser = ActivityLog.builder()
                .id(101L)
                .action("DELETE")
                .build();
                
        when(activityLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(logNoUser));

        byte[] csvBytes = activityLogService.exportActivityLogsToCsv(null, null, null, null, null, null);
        assertThat(csvBytes).isNotEmpty();
    }

    @Test
    void exportActivityLogsToCsv_QuotesAndCommas_EscapesProperly() {
        ActivityLog logWithQuotes = ActivityLog.builder()
                .id(101L)
                .description("Desc with , and \"quote\"")
                .build();
                
        when(activityLogRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(logWithQuotes));

        byte[] csvBytes = activityLogService.exportActivityLogsToCsv(null, null, null, null, null, null);
        String csvContent = new String(csvBytes);
        assertThat(csvContent).contains("\"Desc with , and \"\"quote\"\"\"");
    }

    @Test
    void logActivity_ValidDataWithMetadata_SavesLog() throws Exception {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(objectMapper.writeValueAsString(anyMap())).thenReturn("{\"key\":\"value\"}");

        activityLogService.logActivity(1L, "UPDATE", "USER", 2L, "Updated user", "10.0.0.1", Map.of("key", "value"));

        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void logActivity_UserNotFound_ThrowsException() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
                activityLogService.logActivity(1L, "UPDATE", "USER", 2L, "Desc", "10.0.0.1", null));
    }

    @Test
    void logActivity_JsonProcessingException_SavesWithoutMetadata() throws Exception {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(objectMapper.writeValueAsString(anyMap())).thenThrow(new JsonProcessingException("Error") {});

        activityLogService.logActivity(1L, "UPDATE", "USER", 2L, "Desc", "10.0.0.1", Map.of("key", "value"));

        verify(activityLogRepository).save(argThat(log -> log.getMetadata() == null));
    }

    @Test
    void logActivity_FromRequest_UsesXForwardedFor() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        activityLogService.logActivity("test@example.com", "LOGIN", "SYSTEM", null, "User login", request);

        verify(activityLogRepository).save(argThat(log -> log.getIpAddress().equals("192.168.1.1")));
    }
    
    @Test
    void logActivity_FromRequest_UsesRemoteAddrIfNoHeaders() {
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        activityLogService.logActivity("test@example.com", "LOGIN", "SYSTEM", null, "User login", request);

        verify(activityLogRepository).save(argThat(log -> log.getIpAddress().equals("172.16.0.1")));
    }
}