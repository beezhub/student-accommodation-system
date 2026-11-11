package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.event.DocumentExpiryReminderEvent;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import com.beezhub.student_accommodation.service.document.DocumentExpiryReminderService.Reminder;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExpiryReminderServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final LocalDate EXPIRY = LocalDate.of(2026, 11, 30);

    @Mock ApplicationRepository applicationRepository;
    @Mock RoomAllocationRepository roomAllocationRepository;
    @Mock DocumentRepository documentRepository;
    @Mock DocumentExpiryPolicy documentExpiryPolicy;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TransactionTemplate transactionTemplate;

    private DocumentExpiryReminderService service;
    private final List<Long> tenantsSeen = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new DocumentExpiryReminderService(applicationRepository, roomAllocationRepository,
                documentRepository, documentExpiryPolicy, eventPublisher, transactionTemplate);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(i -> {
            tenantsSeen.add(TenantContext.getTenantId());
            return ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null);
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void reminderDue_firstDayOfThePeriod_thenWeekly_thenAfterExpiry() {
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.minusDays(31), EXPIRY, 30)).isEqualTo(Reminder.NONE);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.minusDays(30), EXPIRY, 30)).isEqualTo(Reminder.EXPIRES_SOON);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.minusDays(29), EXPIRY, 30)).isEqualTo(Reminder.NONE);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.minusDays(23), EXPIRY, 30)).isEqualTo(Reminder.EXPIRES_SOON);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY, EXPIRY, 30)).isEqualTo(Reminder.NONE);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.plusDays(1), EXPIRY, 30)).isEqualTo(Reminder.EXPIRED);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.plusDays(2), EXPIRY, 30)).isEqualTo(Reminder.NONE);
        assertThat(DocumentExpiryReminderService.reminderDue(EXPIRY.plusDays(8), EXPIRY, 30)).isEqualTo(Reminder.EXPIRED);
    }

    @Test
    void runDaily_documentEnteringTheReminderPeriod_remindsUnderTheCompanysTenant() {
        CompanyRules rules = givenCompany(7L, true);
        Document proof = givenApplicantWithDocument(7L);
        when(rules.expiryOf(proof, null)).thenReturn(Optional.of(TODAY.plusDays(30)));

        int reminded = service.runDaily();

        assertThat(reminded).isEqualTo(1);
        assertThat(tenantsSeen).containsExactly(7L);
        ArgumentCaptor<DocumentExpiryReminderEvent> event = ArgumentCaptor.forClass(DocumentExpiryReminderEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new DocumentExpiryReminderEvent(proof.getId(), 7L, TODAY.plusDays(30), false));
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void runDaily_sameExpiryAtTwoCompanies_remindsOnce() {
        when(applicationRepository.findTenantIdsWithStatusIn(any())).thenReturn(List.of(7L, 8L));
        Document proof = document(10L);
        for (Long tenantId : List.of(7L, 8L)) {
            CompanyRules rules = rules(tenantId, true);
            when(rules.expiryOf(proof, null)).thenReturn(Optional.of(TODAY.minusDays(1)));
            when(applicationRepository.findByTenantIdAndStatusInWithStudent(eq(tenantId), any()))
                    .thenReturn(List.of(Application.builder().student(proof.getStudent()).build()));
        }
        when(documentRepository.findAllByStudent_IdIn(anyList())).thenReturn(List.of(proof));

        assertThat(service.runDaily()).isEqualTo(1);
        verify(eventPublisher, times(1)).publishEvent(any(DocumentExpiryReminderEvent.class));
    }

    @Test
    void runDaily_companyWithoutExpiringTypes_readsNoStudents() {
        when(applicationRepository.findTenantIdsWithStatusIn(any())).thenReturn(List.of(7L));
        rules(7L, false);

        assertThat(service.runDaily()).isZero();
        verify(applicationRepository, never()).findByTenantIdAndStatusInWithStudent(any(), any());
    }

    @Test
    void runDaily_platformTenantAndFailures_areSkipped() {
        when(applicationRepository.findTenantIdsWithStatusIn(any())).thenReturn(List.of(0L, 7L, 8L));
        when(documentExpiryPolicy.forCompany(7L)).thenThrow(new IllegalStateException("boom"));
        rules(8L, false);

        assertThat(service.runDaily()).isZero();
        assertThat(tenantsSeen).containsExactly(7L, 8L);
    }

    @Test
    void runDaily_residentWithARoom_isRemindedThroughTheAllocation() {
        when(roomAllocationRepository.findTenantIdsWithStatusIn(any())).thenReturn(List.of(7L));
        CompanyRules rules = rules(7L, true);
        Document proof = document(10L);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(proof.getStudent());
        when(roomAllocationRepository.findByTenantIdAndStatusInWithStudent(eq(7L), any())).thenReturn(List.of(allocation));
        when(documentRepository.findAllByStudent_IdIn(List.of(10L))).thenReturn(List.of(proof));
        when(rules.expiryOf(proof, null)).thenReturn(Optional.of(TODAY.minusDays(8)));

        assertThat(service.runDaily()).isEqualTo(1);
    }

    @Test
    void runDaily_noReminderDueToday_sendsNothing() {
        CompanyRules rules = givenCompany(7L, true);
        Document proof = givenApplicantWithDocument(7L);
        when(rules.expiryOf(proof, null)).thenReturn(Optional.of(TODAY.plusDays(100)));

        assertThat(service.runDaily()).isZero();
        verify(eventPublisher, never()).publishEvent(any());
    }

    private CompanyRules givenCompany(Long tenantId, boolean expiring) {
        when(applicationRepository.findTenantIdsWithStatusIn(any())).thenReturn(List.of(tenantId));
        return rules(tenantId, expiring);
    }

    private CompanyRules rules(Long tenantId, boolean expiring) {
        CompanyRules rules = mock(CompanyRules.class);
        when(documentExpiryPolicy.forCompany(tenantId)).thenReturn(rules);
        when(rules.hasExpiringTypes()).thenReturn(expiring);
        lenient().when(rules.today()).thenReturn(TODAY);
        lenient().when(rules.reminderDays()).thenReturn(30);
        return rules;
    }

    private Document givenApplicantWithDocument(Long tenantId) {
        Document proof = document(10L);
        when(applicationRepository.findByTenantIdAndStatusInWithStudent(eq(tenantId), any()))
                .thenReturn(List.of(Application.builder().student(proof.getStudent()).build()));
        when(documentRepository.findAllByStudent_IdIn(List.of(10L))).thenReturn(List.of(proof));
        return proof;
    }

    private static Document document(Long studentId) {
        Student student = new Student();
        student.setId(studentId);
        DocumentType type = new DocumentType();
        type.setTypeName("Proof of Registration");
        return Document.builder().id(50L).student(student).documentType(type).build();
    }
}
