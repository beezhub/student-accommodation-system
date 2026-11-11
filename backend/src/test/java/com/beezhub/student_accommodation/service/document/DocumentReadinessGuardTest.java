package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.DocumentsNotReadyException;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.DocumentIssue;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.Problem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentReadinessGuardTest {

    @Mock DocumentExpiryPolicy documentExpiryPolicy;
    @Mock DocumentRepository documentRepository;
    @InjectMocks DocumentReadinessGuard guard;

    private final CompanyRules rules = mock(CompanyRules.class);
    private final Student student = new Student();
    private final List<Document> pack = List.of(new Document());

    @BeforeEach
    void setUp() {
        student.setId(10L);
        lenient().when(documentExpiryPolicy.forCompany(7L)).thenReturn(rules);
        lenient().when(rules.hasExpiringTypes()).thenReturn(true);
        lenient().when(documentRepository.findAllByStudent_Id(10L)).thenReturn(pack);
    }

    @Test
    void assertReadyForRoom_expiredDocument_refusesTheOffer() {
        when(rules.issuesForRoom(pack, null)).thenReturn(List.of(
                new DocumentIssue("Proof of Registration", Problem.EXPIRED, LocalDate.of(2026, 11, 30))));

        assertThatThrownBy(() -> guard.assertReadyForRoom(student, 7L, Application.builder().build()))
                .isInstanceOf(DocumentsNotReadyException.class)
                .hasMessageContaining("Proof of Registration (expired on 30 November 2026)");
    }

    @Test
    void assertReadyForRoom_documentsInOrder_passes() {
        when(rules.issuesForRoom(pack, null)).thenReturn(List.of());

        assertThatCode(() -> guard.assertReadyForRoom(student, 7L, null)).doesNotThrowAnyException();
    }

    @Test
    void issuesForRoom_companyWithoutExpiringTypes_readsNoDocuments() {
        when(rules.hasExpiringTypes()).thenReturn(false);

        assertThat(guard.issuesForRoom(student, 7L, null)).isEmpty();
        verify(documentRepository, never()).findAllByStudent_Id(any());
    }

    @Test
    void assertReadyForCheckIn_pendingDocument_refusesCheckIn() {
        when(rules.issuesForRoom(pack, null)).thenReturn(List.of(
                new DocumentIssue("Proof of Registration", Problem.NOT_APPROVED, null)));

        assertThatThrownBy(() -> guard.assertReadyForCheckIn(allocation()))
                .isInstanceOf(DocumentsNotReadyException.class)
                .hasMessageContaining("cannot check in")
                .hasMessageContaining("Proof of Registration (not yet approved)");
    }

    @Test
    void assertReadyForCheckIn_documentsInOrder_passes() {
        when(rules.issuesForRoom(pack, null)).thenReturn(List.of());

        assertThatCode(() -> guard.assertReadyForCheckIn(allocation())).doesNotThrowAnyException();
    }

    @Test
    void issuesForStay_readsTheAllocationsCompany() {
        DocumentIssue issue = new DocumentIssue("ID Document", Problem.NOT_APPROVED, null);
        when(rules.issuesForStay(pack, null)).thenReturn(List.of(issue));

        assertThat(guard.issuesForStay(allocation())).containsExactly(issue);
    }

    private RoomAllocation allocation() {
        Room room = new Room();
        room.setTenantId(7L);
        RoomAllocation allocation = new RoomAllocation();
        allocation.setId(40L);
        allocation.setTenantId(7L);
        allocation.setRoom(room);
        allocation.setStudent(student);
        return allocation;
    }
}
