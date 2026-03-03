package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ActivityLogPageResponse;
import com.beezhub.student_accommodation.model.dto.ActivityLogResponse;
import com.beezhub.student_accommodation.model.entity.ActivityLog;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ActivityLogRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get activity logs with filters and pagination
     */
    @Transactional(readOnly = true)
    public ActivityLogPageResponse getActivityLogs(
            Long userId,
            String userRole,
            String action,
            String entityType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        log.debug("Fetching activity logs with filters - userRole: {}, action: {}, entityType: {}",
                userRole, action, entityType);

        Specification<ActivityLog> spec = buildSpec(userId, userRole, action, entityType, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ActivityLog> logPage = activityLogRepository.findAll(spec, pageable);

        List<ActivityLogResponse> content = logPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ActivityLogPageResponse.builder()
                .content(content)
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .size(logPage.getSize())
                .number(logPage.getNumber())
                .build();
    }

    /**
     * Get a specific activity log by ID
     */
    @Transactional(readOnly = true)
    public ActivityLogResponse getActivityLogById(Long id) {
        log.debug("Fetching activity log with ID: {}", id);

        ActivityLog logEntry = activityLogRepository.findById(id)
                .orElseThrow(() -> new com.beezhub.student_accommodation.exceptions.ResourceNotFoundException("ActivityLog", "id", id));

        return mapToResponse(logEntry);
    }

    /**
     * Export activity logs to CSV
     */
    @Transactional(readOnly = true)
    public byte[] exportActivityLogsToCsv(
            Long userId,
            String userRole,
            String action,
            String entityType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("Exporting activity logs to CSV");

        Specification<ActivityLog> spec = buildSpec(userId, userRole, action, entityType, startDate, endDate);

        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        List<ActivityLog> logs = activityLogRepository.findAll(spec, sort);

        return generateCsv(logs);
    }

    /**
     * Log a user activity
     */
    @Transactional
    public void logActivity(
            Long userId,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress,
            Map<String, Object> metadata
    ) {
        log.debug("Logging activity - user: {}, action: {}, entityType: {}", userId, action, entityType);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize activity metadata", e);
            }
        }

        ActivityLog activityLog = ActivityLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ipAddress)
                .metadata(metadataJson)
                .build();

        activityLogRepository.save(activityLog);
        log.info("Activity logged: {} - {} on {} {}", user.getEmail(), action, entityType, entityId);
    }

    /**
     * Helper method to log activity from HttpServletRequest
     */
    public void logActivity(
            String userEmail,
            String action,
            String entityType,
            Long entityId,
            String description,
            HttpServletRequest request
    ) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String ipAddress = getClientIpAddress(request);

        logActivity(user.getId(), action, entityType, entityId, description, ipAddress, null);
    }

    /**
     * Map entity to response DTO
     */
    private ActivityLogResponse mapToResponse(ActivityLog log) {
        AppUser user = log.getUser();

        Map<String, Object> metadataMap = null;
        if (log.getMetadata() != null) {
            try {
                metadataMap = objectMapper.readValue(
                        log.getMetadata(),
                        new TypeReference<>() {}
                );
            } catch (JsonProcessingException e) {
                ActivityLogService.log.warn("Failed to deserialize activity log metadata", e);
            }
        }

        String userEmail = user != null ? user.getEmail() : null;
        String firstName = user != null ? user.getFirstName() : null;
        String lastName = user != null ? user.getLastName() : null;
        String fullName = (
                (firstName == null ? "" : firstName.trim()) +
                        (lastName == null || lastName.trim().isEmpty() ? "" : " " + lastName.trim())
        ).trim();

        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(user != null ? user.getId() : null)
                .userEmail(userEmail)
                .userFullName(fullName.isEmpty() ? null : fullName)
                .userRole(user != null && user.getUserRole() != null ? user.getUserRole().name() : null)
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .metadata(metadataMap)
                .build();
    }

    /**
     * Generate CSV from activity logs
     */
    private byte[] generateCsv(List<ActivityLog> logs) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // CSV Header
            writer.println("ID,Timestamp,User Email,User Name,User Role,Action,Entity Type,Entity ID,Description,IP Address");

            // CSV Rows
            for (ActivityLog log : logs) {
                AppUser user = log.getUser();

                writer.print(csvCell(log.getId() != null ? log.getId().toString() : ""));
                writer.print(',');
                writer.print(csvCell(log.getTimestamp() != null ? log.getTimestamp().toString() : ""));
                writer.print(',');
                writer.print(csvCell(user != null ? user.getEmail() : ""));
                writer.print(',');
                writer.print(csvCell(user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : ""));
                writer.print(',');
                writer.print(csvCell(user != null && user.getUserRole() != null ? user.getUserRole().name() : ""));
                writer.print(',');
                writer.print(csvCell(log.getAction()));
                writer.print(',');
                writer.print(csvCell(log.getEntityType()));
                writer.print(',');
                writer.print(csvCell(log.getEntityId() != null ? log.getEntityId().toString() : ""));
                writer.print(',');
                writer.print(csvCell(log.getDescription()));
                writer.print(',');
                writer.print(csvCell(log.getIpAddress()));
                writer.println();
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate CSV", e);
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    /**
     * RFC4180-ish: escape quotes by doubling them and quote the whole cell if it contains comma/quote/CR/LF.
     */
    private String csvCell(String value) {
        if (value == null) return "";
        String v = value;
        boolean mustQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        v = v.replace("\"", "\"\"");
        return mustQuote ? "\"" + v + "\"" : v;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs (take the first one)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private Specification<ActivityLog> buildSpec(
            Long userId,
            String userRole,
            String action,
            String entityType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate");
        }

        // Start with a no-op spec (always true) and chain optional filters with .and(...)
        Specification<ActivityLog> spec = (root, query, cb) -> cb.conjunction();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("user").get("id"), userId));
        }

        if (userRole != null && !userRole.trim().isEmpty()) {
            UserRole parsedRole;
            // Accept both enum name (ADMIN) and display role (Admin)
            try {
                parsedRole = UserRole.valueOf(userRole.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                parsedRole = UserRole.enumRoleOf(userRole.trim());
            }
            UserRole finalParsedRole = parsedRole;

            spec = spec.and((root, query, cb) -> cb.equal(root.join("user").get("userRole"), finalParsedRole));
        }

        if (action != null && !action.trim().isEmpty()) {
            String a = action.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), a));
        }

        if (entityType != null && !entityType.trim().isEmpty()) {
            String e = entityType.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), e));
        }

        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), startDateTime));
        }

        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), endDateTime));
        }

        return spec;
    }
}
