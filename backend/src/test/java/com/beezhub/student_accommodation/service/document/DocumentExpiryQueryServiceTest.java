package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.model.dto.DocumentExpiryResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExpiryQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Mock DocumentExpiryPolicy documentExpiryPolicy;
    @Mock DocumentRepository documentRepository;
    @Mock ApplicationRepository applicationRepository;
    @Mock RoomAllocationRepository roomAllocationRepository;
    @Mock StudentRepository studentRepository;
    @Mock AuthService authService;
    @InjectMocks DocumentExpiryQueryService service;

    private final Student student = new Student();
    private final Document proof = Document.builder().id(50L).documentType(type()).build();

    @BeforeEach
    void setUp() {
        student.setId(10L);
        proof.setStudent(student);
        lenient().when(documentRepository.findAllByStudent_Id(10L)).thenReturn(List.of(proof));
    }

    @Test
    void getMyDocumentExpiries_twoCompanies_givesTheEarliestDate() {
        AppUser user = AppUser.builder().id(1L).build();
        when(authService.getCurrentUser()).thenReturn(user);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(eq(10L), any()))
                .thenReturn(List.of(Application.builder().tenantId(7L).student(student).build()));
        RoomAllocation room = new RoomAllocation();
        room.setTenantId(8L);
        when(roomAllocationRepository.findLiveAllocationsByStudentId(10L)).thenReturn(List.of(room));
        rules(7L, LocalDate.of(2026, 12, 10));
        CompanyRules earlier = rules(8L, LocalDate.of(2026, 11, 30));
        when(earlier.isDueForReplacement(proof, null)).thenReturn(true);

        List<DocumentExpiryResponse> expiries = service.getMyDocumentExpiries();

        assertThat(expiries).singleElement().satisfies(expiry -> {
            assertThat(expiry.getExpiryDate()).isEqualTo(LocalDate.of(2026, 11, 30));
            assertThat(expiry.getExpired()).isFalse();
            assertThat(expiry.getDueForReplacement()).isTrue();
            assertThat(expiry.getDocumentTypeName()).isEqualTo("Proof of Registration");
        });
    }

    @Test
    void getMyDocumentExpiries_noApplicationsOrPlaces_isEmpty() {
        AppUser user = AppUser.builder().id(1L).build();
        when(authService.getCurrentUser()).thenReturn(user);
        when(studentRepository.findByAppUser_Id(1L)).thenReturn(Optional.of(student));

        assertThat(service.getMyDocumentExpiries()).isEmpty();
    }

    @Test
    void expiriesFor_marksAnExpiredDocument() {
        rules(7L, LocalDate.of(2026, 11, 1));

        Map<Long, DocumentExpiryResponse> expiries =
                service.expiriesFor(Application.builder().tenantId(7L).student(student).build());

        assertThat(expiries.get(50L).getExpired()).isTrue();
    }

    @Test
    void expiredByApplication_readsEachApplicationsCompany() {
        CompanyRules company = rules(7L, LocalDate.of(2026, 11, 1));
        when(company.isExpired(proof, null)).thenReturn(true);
        CompanyRules other = mock(CompanyRules.class);
        when(documentExpiryPolicy.forCompany(8L)).thenReturn(other);
        Application atCompany = Application.builder().id(1L).tenantId(7L).student(student).build();
        Application atOther = Application.builder().id(2L).tenantId(8L).student(student).build();

        Map<Long, Boolean> expired = service.expiredByApplication(List.of(atCompany, atOther),
                Map.of(10L, List.of(proof)));

        assertThat(expired).containsEntry(1L, true).doesNotContainKey(2L);
    }

    private CompanyRules rules(Long tenantId, LocalDate expiry) {
        CompanyRules rules = mock(CompanyRules.class);
        when(documentExpiryPolicy.forCompany(tenantId)).thenReturn(rules);
        lenient().when(rules.hasExpiringTypes()).thenReturn(true);
        lenient().when(rules.today()).thenReturn(TODAY);
        lenient().when(rules.expiryOf(proof, null)).thenReturn(Optional.of(expiry));
        return rules;
    }

    private static DocumentType type() {
        DocumentType type = new DocumentType();
        type.setId(2L);
        type.setTypeName("Proof of Registration");
        return type;
    }
}
