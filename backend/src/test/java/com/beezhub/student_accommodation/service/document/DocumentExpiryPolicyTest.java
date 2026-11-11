package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import com.beezhub.student_accommodation.service.SystemSettingService;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.DocumentIssue;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.Problem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExpiryPolicyTest {

    private static final ZoneId SA = ZoneId.of("Africa/Johannesburg");
    private static final Long COMPANY = 7L;
    private static final Long INSTITUTION = 3L;
    private static final LocalDate YEAR_END = LocalDate.of(2026, 11, 30);

    @Mock DocumentTypeService documentTypeService;
    @Mock AcademicYearService academicYearService;
    @Mock SystemSettingService systemSettingService;

    private DocumentExpiryPolicy policy;

    @BeforeEach
    void setUp() {
        policyOn(LocalDate.of(2026, 11, 15));
        lenient().when(documentTypeService.getEffectiveDocumentTypes(COMPANY)).thenReturn(List.of(
                type("Proof of Registration", true, true),
                type("ID Document", true, false),
                type("Bank Statement", false, true)));
        lenient().when(academicYearService.findCompanyYearOn(eq(COMPANY), eq(INSTITUTION), any()))
                .thenReturn(Optional.of(AcademicYear.builder().endDate(YEAR_END).build()));
        lenient().when(systemSettingService.getSettingForTenant(COMPANY, DocumentExpiryPolicy.REMINDER_DAYS_KEY))
                .thenReturn("30");
    }

    private void policyOn(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(SA).plusHours(9).toInstant(), SA);
        policy = new DocumentExpiryPolicy(documentTypeService, academicYearService, systemSettingService, clock);
    }

    @Test
    void expiryOf_expiringType_isTheYearEndOfTheUploadDay() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 10, 12, 0);

        Optional<LocalDate> expiry = policy.forCompany(COMPANY).expiryOf("proof of registration", june, INSTITUTION);

        assertThat(expiry).contains(YEAR_END);
        verify(academicYearService).findCompanyYearOn(COMPANY, INSTITUTION, LocalDate.of(2026, 6, 10));
    }

    @Test
    void expiryOf_typeNotExpiring_hasNone() {
        assertThat(policy.forCompany(COMPANY).expiryOf("ID Document", LocalDateTime.now(), INSTITUTION)).isEmpty();
    }

    @Test
    void expiryOf_noYearForTheUploadDay_hasNone() {
        when(academicYearService.findCompanyYearOn(eq(COMPANY), eq(9L), any())).thenReturn(Optional.empty());

        assertThat(policy.forCompany(COMPANY).expiryOf("Proof of Registration",
                LocalDateTime.of(2026, 6, 10, 12, 0), 9L)).isEmpty();
    }

    @Test
    void expiryOf_sameUploadDayTwice_looksTheYearUpOnce() {
        CompanyRules rules = policy.forCompany(COMPANY);
        LocalDateTime june = LocalDateTime.of(2026, 6, 10, 12, 0);

        rules.expiryOf("Proof of Registration", june, INSTITUTION);
        rules.expiryOf("Bank Statement", june.plusHours(2), INSTITUTION);

        verify(academicYearService, times(1)).findCompanyYearOn(any(), any(), any());
    }

    @Test
    void isExpired_onTheExpiryDay_stillValid_andTheDayAfter_expired() {
        Document proof = document("Proof of Registration", DocumentStatus.APPROVED);
        policyOn(YEAR_END);
        assertThat(policy.forCompany(COMPANY).isExpired(proof, INSTITUTION)).isFalse();

        policyOn(YEAR_END.plusDays(1));
        assertThat(policy.forCompany(COMPANY).isExpired(proof, INSTITUTION)).isTrue();
    }

    @Test
    void isDueForReplacement_expired_soonToExpire_andCurrent() {
        Document proof = document("Proof of Registration", DocumentStatus.APPROVED);

        policyOn(YEAR_END.plusDays(3));
        assertThat(policy.forCompany(COMPANY).isDueForReplacement(proof, INSTITUTION)).isTrue();

        policyOn(YEAR_END.minusDays(30));
        assertThat(policy.forCompany(COMPANY).isDueForReplacement(proof, INSTITUTION)).isTrue();

        policyOn(YEAR_END.minusDays(31));
        assertThat(policy.forCompany(COMPANY).isDueForReplacement(proof, INSTITUTION)).isFalse();
    }

    @Test
    void issuesForRoom_onlyRequiredExpiringTypesCount() {
        policyOn(YEAR_END.plusDays(1));
        CompanyRules rules = policy.forCompany(COMPANY);

        List<DocumentIssue> expired = rules.issuesForRoom(List.of(
                document("Proof of Registration", DocumentStatus.APPROVED),
                document("ID Document", DocumentStatus.PENDING)), INSTITUTION);
        List<DocumentIssue> pending = rules.issuesForRoom(List.of(
                document("Proof of Registration", DocumentStatus.PENDING)), INSTITUTION);
        List<DocumentIssue> missing = rules.issuesForRoom(List.of(), INSTITUTION);

        assertThat(expired).extracting(DocumentIssue::problem).containsExactly(Problem.EXPIRED);
        assertThat(expired.get(0).describe()).isEqualTo("Proof of Registration (expired on 30 November 2026)");
        assertThat(pending).extracting(DocumentIssue::problem).containsExactly(Problem.NOT_APPROVED);
        assertThat(missing).extracting(DocumentIssue::describe).containsExactly("Proof of Registration (not uploaded)");
    }

    @Test
    void issuesForRoom_approvedAndCurrent_none() {
        assertThat(policy.forCompany(COMPANY).issuesForRoom(List.of(
                document("Proof of Registration", DocumentStatus.APPROVED)), INSTITUTION)).isEmpty();
    }

    @Test
    void issuesForStay_everyRequiredDocumentHasToBeApproved() {
        List<DocumentIssue> issues = policy.forCompany(COMPANY).issuesForStay(List.of(
                document("Proof of Registration", DocumentStatus.APPROVED),
                document("ID Document", DocumentStatus.PENDING)), INSTITUTION);

        assertThat(issues).extracting(DocumentIssue::describe).containsExactly("ID Document (not yet approved)");
    }

    @Test
    void forCompany_noExpiringTypes_saysSo() {
        when(documentTypeService.getEffectiveDocumentTypes(8L)).thenReturn(List.of(type("ID Document", true, false)));

        assertThat(policy.forCompany(8L).hasExpiringTypes()).isFalse();
        assertThat(policy.forCompany(COMPANY).hasExpiringTypes()).isTrue();
    }

    @Test
    void reminderDays_missingInvalidOrNegative_fallsBackToTheDefault() {
        when(systemSettingService.getSettingForTenant(1L, DocumentExpiryPolicy.REMINDER_DAYS_KEY)).thenReturn(null);
        when(systemSettingService.getSettingForTenant(2L, DocumentExpiryPolicy.REMINDER_DAYS_KEY)).thenReturn("soon");
        when(systemSettingService.getSettingForTenant(3L, DocumentExpiryPolicy.REMINDER_DAYS_KEY)).thenReturn("-4");
        when(systemSettingService.getSettingForTenant(4L, DocumentExpiryPolicy.REMINDER_DAYS_KEY)).thenReturn(" 14 ");

        assertThat(policy.reminderDays(1L)).isEqualTo(DocumentExpiryPolicy.DEFAULT_REMINDER_DAYS);
        assertThat(policy.reminderDays(2L)).isEqualTo(DocumentExpiryPolicy.DEFAULT_REMINDER_DAYS);
        assertThat(policy.reminderDays(3L)).isEqualTo(DocumentExpiryPolicy.DEFAULT_REMINDER_DAYS);
        assertThat(policy.reminderDays(4L)).isEqualTo(14);
    }

    @Test
    void companyOf_platformStampedApplication_usesTheBuildingsCompany() {
        Building building = new Building();
        building.setTenantId(7L);
        Application platformStamped = Application.builder().tenantId(0L).preferredBuilding(building).build();

        assertThat(DocumentExpiryPolicy.companyOf(Application.builder().tenantId(5L).build())).isEqualTo(5L);
        assertThat(DocumentExpiryPolicy.companyOf(platformStamped)).isEqualTo(7L);
        assertThat(DocumentExpiryPolicy.companyOf(null)).isNull();
    }

    @Test
    void institutionOf_application_thenStudentProfile() {
        Institution college = new Institution();
        college.setId(4L);
        Institution university = new Institution();
        university.setId(5L);
        Student student = new Student();
        student.setInstitution(university);

        assertThat(DocumentExpiryPolicy.institutionOf(Application.builder().institution(college).build(), student))
                .isEqualTo(4L);
        assertThat(DocumentExpiryPolicy.institutionOf(Application.builder().build(), student)).isEqualTo(5L);
        assertThat(DocumentExpiryPolicy.institutionOf(null, null)).isNull();
    }

    static DocumentType type(String name, boolean required, boolean expiring) {
        DocumentType type = new DocumentType();
        type.setTypeName(name);
        type.setIsRequired(required);
        type.setIsExpiring(expiring);
        type.setTenantId(COMPANY);
        return type;
    }

    static Document document(String typeName, DocumentStatus status) {
        return Document.builder().id((long) typeName.hashCode()).documentType(type(typeName, true, false))
                .documentStatus(status).createdAt(LocalDateTime.of(2026, 6, 10, 12, 0)).build();
    }
}
