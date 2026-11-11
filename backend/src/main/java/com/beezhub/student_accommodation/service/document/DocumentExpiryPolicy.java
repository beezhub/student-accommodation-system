package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import com.beezhub.student_accommodation.service.SystemSettingService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * When a student's document expires, for one company (GH-498).
 *
 * <p>A company marks a document type as expiring. The expiry is not stored: it is the end date of
 * the company's first academic year, for the student's institution, that ends on or after the
 * upload date, picked like the current year (a year containing the upload date first, then the
 * institution's own year over an "All institutions" one). A company with no such year gives no
 * expiry until it creates one. Worked out per company because a student's document pack is shared
 * by every company they apply to, and two companies can end their years on different dates.
 *
 * <p>Types are matched by name over the company's merged view, the way
 * {@code DocumentTypeService.getEffectiveRequiredTypeNames} does, so a company's own copy of a
 * platform type decides. A document is valid up to and including its expiry date.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExpiryPolicy {

    public static final String REMINDER_DAYS_KEY = "document_expiry_reminder_days";
    static final int DEFAULT_REMINDER_DAYS = 30;
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final DocumentTypeService documentTypeService;
    private final AcademicYearService academicYearService;
    private final SystemSettingService systemSettingService;
    private final Clock clock;

    /** Today in South African time, the day every expiry is read against. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** One company's rules, loaded once and reused for every document read against them. */
    public CompanyRules forCompany(Long tenantId) {
        Long company = tenantId == null ? TenantContext.PLATFORM_TENANT_ID : tenantId;
        Map<String, String> required = new LinkedHashMap<>();
        Map<String, String> expiring = new LinkedHashMap<>();
        for (DocumentType type : documentTypeService.getEffectiveDocumentTypes(company)) {
            String key = type.getTypeName().toLowerCase();
            if (Boolean.TRUE.equals(type.getIsRequired())) {
                required.put(key, type.getTypeName());
            }
            if (Boolean.TRUE.equals(type.getIsExpiring())) {
                expiring.put(key, type.getTypeName());
            }
        }
        return new CompanyRules(company, required, expiring, today());
    }

    /** Days before expiry a student is reminded, and may replace the document. */
    public int reminderDays(Long tenantId) {
        String value = systemSettingService.getSettingForTenant(tenantId, REMINDER_DAYS_KEY);
        if (value == null || value.isBlank()) {
            return DEFAULT_REMINDER_DAYS;
        }
        try {
            int days = Integer.parseInt(value.trim());
            return days < 0 ? DEFAULT_REMINDER_DAYS : days;
        } catch (NumberFormatException e) {
            log.warn("action=documentExpirySettingInvalid tenantId={} key={}", tenantId, REMINDER_DAYS_KEY);
            return DEFAULT_REMINDER_DAYS;
        }
    }

    /**
     * The company an application is with. Applications created by a platform admin are stamped
     * with the platform tenant (GH-146); the building applied to still names the company.
     */
    public static Long companyOf(Application application) {
        if (application == null) {
            return null;
        }
        if (application.getTenantId() != null && !TenantContext.PLATFORM_TENANT_ID.equals(application.getTenantId())) {
            return application.getTenantId();
        }
        return application.getPreferredBuilding() != null ? application.getPreferredBuilding().getTenantId()
                : application.getTenantId();
    }

    /** The institution an application was made for, else the student's profile institution. */
    public static Long institutionOf(Application application, Student student) {
        if (application != null && application.getInstitution() != null) {
            return application.getInstitution().getId();
        }
        Student owner = student != null ? student : application != null ? application.getStudent() : null;
        return owner != null && owner.getInstitution() != null ? owner.getInstitution().getId() : null;
    }

    /** Why a document stops an offer, a check-in or a stay extension. */
    public enum Problem { MISSING, NOT_APPROVED, EXPIRED }

    /** One document in the way, with its expiry date when it has one. */
    public record DocumentIssue(String typeName, Problem problem, LocalDate expiryDate) {

        public String describe() {
            return switch (problem) {
                case MISSING -> typeName + " (not uploaded)";
                case NOT_APPROVED -> typeName + " (not yet approved)";
                case EXPIRED -> typeName + " (expired on " + expiryDate.format(DAY) + ")";
            };
        }
    }

    /** The rules of one company on one day. */
    public final class CompanyRules {

        private final Long tenantId;
        private final Map<String, String> required;
        private final Map<String, String> expiring;
        private Integer reminderDays;
        private final LocalDate today;
        private final Map<String, Optional<LocalDate>> yearEnds = new HashMap<>();

        private CompanyRules(Long tenantId, Map<String, String> required, Map<String, String> expiring,
                LocalDate today) {
            this.tenantId = tenantId;
            this.required = required;
            this.expiring = expiring;
            this.today = today;
        }

        public Long tenantId() {
            return tenantId;
        }

        /** Read from the settings cascade on first use only. */
        public int reminderDays() {
            if (reminderDays == null) {
                reminderDays = DocumentExpiryPolicy.this.reminderDays(tenantId);
            }
            return reminderDays;
        }

        public LocalDate today() {
            return today;
        }

        public boolean hasExpiringTypes() {
            return !expiring.isEmpty();
        }

        public boolean isExpiring(String typeName) {
            return typeName != null && expiring.containsKey(typeName.toLowerCase());
        }

        /** The expiry of a document of {@code typeName} uploaded at {@code uploadedAt}, if it has one. */
        public Optional<LocalDate> expiryOf(String typeName, LocalDateTime uploadedAt, Long institutionId) {
            if (!isExpiring(typeName) || uploadedAt == null) {
                return Optional.empty();
            }
            LocalDate uploaded = uploadedAt.toLocalDate();
            return yearEnds.computeIfAbsent(institutionId + "|" + uploaded, key -> academicYearService
                    .findCompanyYearOn(tenantId, institutionId, uploaded).map(AcademicYear::getEndDate));
        }

        public Optional<LocalDate> expiryOf(Document document, Long institutionId) {
            return expiryOf(typeName(document), document.getCreatedAt(), institutionId);
        }

        /** Expired: past its expiry date. */
        public boolean isExpired(Document document, Long institutionId) {
            return expiryOf(document, institutionId).map(today::isAfter).orElse(false);
        }

        /** Expired, or expiring within the reminder period: the student may replace it. */
        public boolean isDueForReplacement(Document document, Long institutionId) {
            return expiryOf(document, institutionId)
                    .map(expiry -> !today.isBefore(expiry.minusDays(reminderDays())))
                    .orElse(false);
        }

        /**
         * What stops a room offer or a check-in: a required expiring document that is missing, not
         * yet approved, or expired.
         */
        public List<DocumentIssue> issuesForRoom(List<Document> pack, Long institutionId) {
            return issues(pack, institutionId, false);
        }

        /**
         * What stops a stay extension: any required document missing or not approved, and any
         * required expiring document expired.
         */
        public List<DocumentIssue> issuesForStay(List<Document> pack, Long institutionId) {
            return issues(pack, institutionId, true);
        }

        private List<DocumentIssue> issues(List<Document> pack, Long institutionId, boolean everyRequired) {
            Map<String, Document> byName = new HashMap<>();
            for (Document document : pack) {
                String name = typeName(document);
                if (name != null) {
                    byName.put(name.toLowerCase(), document);
                }
            }
            List<DocumentIssue> issues = new ArrayList<>();
            required.forEach((key, displayName) -> {
                boolean expiringType = expiring.containsKey(key);
                if (!everyRequired && !expiringType) {
                    return;
                }
                Document document = byName.get(key);
                if (document == null) {
                    issues.add(new DocumentIssue(displayName, Problem.MISSING, null));
                } else if (document.getDocumentStatus() != DocumentStatus.APPROVED) {
                    issues.add(new DocumentIssue(displayName, Problem.NOT_APPROVED, null));
                } else if (expiringType) {
                    expiryOf(document, institutionId).filter(today::isAfter).ifPresent(expiry ->
                            issues.add(new DocumentIssue(displayName, Problem.EXPIRED, expiry)));
                }
            });
            return issues;
        }
    }

    private static String typeName(Document document) {
        DocumentType type = document == null ? null : document.getDocumentType();
        return type == null ? null : type.getTypeName();
    }
}
