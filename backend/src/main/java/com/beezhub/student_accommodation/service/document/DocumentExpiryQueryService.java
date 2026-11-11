package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.StudentNotFoundException;
import com.beezhub.student_accommodation.model.dto.DocumentExpiryResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.document.DocumentExpiryPolicy.CompanyRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads document expiry for screens (GH-498): the student's own documents page, and the expired
 * badge on the admin application list and detail.
 */
@Service
@RequiredArgsConstructor
public class DocumentExpiryQueryService {

    private final DocumentExpiryPolicy documentExpiryPolicy;
    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final StudentRepository studentRepository;
    private final AuthService authService;

    /** A company and the institution a student's link to it is for. */
    private record Link(Long tenantId, Long institutionId) {
    }

    /**
     * The calling student's expiring documents, read against every company they are applying to
     * or hold a place with, the earliest expiry first.
     */
    @Transactional(readOnly = true)
    public List<DocumentExpiryResponse> getMyDocumentExpiries() {
        AppUser user = authService.getCurrentUser();
        Student student = studentRepository.findByAppUser_Id(user.getId())
                .orElseThrow(() -> new StudentNotFoundException("Student not found for user ID: " + user.getId()));
        List<Link> links = linksOf(student);
        if (links.isEmpty()) {
            return List.of();
        }
        Map<Long, CompanyRules> rules = new HashMap<>();
        List<DocumentExpiryResponse> expiries = new ArrayList<>();
        for (Document document : documentRepository.findAllByStudent_Id(student.getId())) {
            DocumentExpiryResponse earliest = null;
            for (Link link : links) {
                CompanyRules company = rules.computeIfAbsent(link.tenantId(), documentExpiryPolicy::forCompany);
                DocumentExpiryResponse candidate = toResponse(document, company, link.institutionId());
                if (candidate != null && (earliest == null || candidate.getExpiryDate().isBefore(earliest.getExpiryDate()))) {
                    earliest = candidate;
                }
            }
            if (earliest != null) {
                expiries.add(earliest);
            }
        }
        expiries.sort(Comparator.comparing(DocumentExpiryResponse::getExpiryDate));
        return expiries;
    }

    /** The expiring documents of an application's student, by document id, read for its company. */
    @Transactional(readOnly = true)
    public Map<Long, DocumentExpiryResponse> expiriesFor(Application application) {
        Student student = application.getStudent();
        CompanyRules rules = documentExpiryPolicy.forCompany(DocumentExpiryPolicy.companyOf(application));
        if (student == null || !rules.hasExpiringTypes()) {
            return Map.of();
        }
        Long institutionId = DocumentExpiryPolicy.institutionOf(application, student);
        Map<Long, DocumentExpiryResponse> byDocument = new LinkedHashMap<>();
        for (Document document : documentRepository.findAllByStudent_Id(student.getId())) {
            Optional.ofNullable(toResponse(document, rules, institutionId))
                    .ifPresent(response -> byDocument.put(document.getId(), response));
        }
        return byDocument;
    }

    /**
     * Whether each application's student has an expired document at the application's company, by
     * application id, for the admin list. The documents are passed in, already loaded for the page.
     */
    public Map<Long, Boolean> expiredByApplication(List<Application> applications,
            Map<Long, List<Document>> documentsByStudent) {
        Map<Long, CompanyRules> rules = new HashMap<>();
        Map<Long, Boolean> expired = new HashMap<>();
        for (Application application : applications) {
            CompanyRules company = rules.computeIfAbsent(DocumentExpiryPolicy.companyOf(application),
                    documentExpiryPolicy::forCompany);
            if (!company.hasExpiringTypes() || application.getStudent() == null) {
                continue;
            }
            Long institutionId = DocumentExpiryPolicy.institutionOf(application, application.getStudent());
            boolean any = documentsByStudent.getOrDefault(application.getStudent().getId(), List.of()).stream()
                    .anyMatch(document -> company.isExpired(document, institutionId));
            expired.put(application.getId(), any);
        }
        return expired;
    }

    private static DocumentExpiryResponse toResponse(Document document, CompanyRules rules, Long institutionId) {
        Optional<LocalDate> expiry = rules.expiryOf(document, institutionId);
        if (expiry.isEmpty()) {
            return null;
        }
        return DocumentExpiryResponse.builder()
                .documentId(document.getId())
                .documentTypeId(document.getDocumentType().getId())
                .documentTypeName(document.getDocumentType().getTypeName())
                .expiryDate(expiry.get())
                .expired(rules.today().isAfter(expiry.get()))
                .dueForReplacement(rules.isDueForReplacement(document, institutionId))
                .build();
    }

    /** The companies a student is applying to or holds a place with, as the reminders reach them. */
    private List<Link> linksOf(Student student) {
        List<Link> links = new ArrayList<>();
        for (Application application : applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(
                student.getId(), DocumentExpiryReminderService.IN_PROGRESS)) {
            links.add(new Link(DocumentExpiryPolicy.companyOf(application),
                    DocumentExpiryPolicy.institutionOf(application, student)));
        }
        for (RoomAllocation allocation : roomAllocationRepository.findLiveAllocationsByStudentId(student.getId())) {
            links.add(new Link(allocation.getTenantId(),
                    DocumentExpiryPolicy.institutionOf(allocation.getApplication(), student)));
        }
        return links.stream().filter(link -> link.tenantId() != null).distinct().toList();
    }
}
