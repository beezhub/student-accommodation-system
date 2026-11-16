package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The document types a student's Documents page offers (GH-443).
 *
 * <p>The submit check and the admin checklist ask for the required types of the company applied
 * to, matched by name. {@code GET /api/document-type} resolves the tenant from the caller, and
 * students are platform tenant 0, so a type a company created under a new name was never offered
 * and the student could not apply there. This adds the required types of the companies the student
 * is applying to: those of the student's live applications, and those owning the buildings picked
 * on the application form, which needs the documents before any application exists.
 *
 * <p>Students cross companies here as they do on the application form (GH-495), and see no more
 * than the form shows them: the required types of a company whose building they picked or that
 * they applied to. A company's optional types, and the types of a company the student names no
 * building of and has no application with, are never returned.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentDocumentTypeService {

    /** As many buildings as one application form takes. */
    static final int MAX_BUILDINGS = 3;

    private final AuthService authService;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final BuildingRepository buildingRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTypeService documentTypeService;
    private final DocumentTypeMapper documentTypeMapper;

    /**
     * One entry per type name: the platform types, then the required types of the companies the
     * student is applying to under a name the platform does not use, then any other type the
     * student already uploaded against. A name the student uploaded under keeps the type of that
     * upload, so the page shows the document in its slot. A name is marked required when the
     * platform or one of those companies requires it.
     *
     * @param buildingIds buildings picked on the application form, at most {@value #MAX_BUILDINGS};
     *                    an id that matches no building is ignored
     */
    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> getUploadableTypes(List<Long> buildingIds) {
        Set<Long> picked = buildingIds == null ? Set.of() : buildingIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (picked.size() > MAX_BUILDINGS) {
            throw new IllegalArgumentException("You can pick at most " + MAX_BUILDINGS + " buildings");
        }
        Optional<Student> student = studentRepository.findByAppUser_Id(authService.getCurrentUser().getId());

        List<DocumentType> platform = documentTypeService.getEffectiveDocumentTypes(TenantContext.PLATFORM_TENANT_ID);
        Set<Long> companies = companiesOf(student, picked);
        List<DocumentType> companyRequired = companies.stream()
                .flatMap(companyId -> documentTypeService.getEffectiveDocumentTypes(companyId).stream())
                .filter(type -> Boolean.TRUE.equals(type.getIsRequired()))
                .toList();
        Map<String, DocumentType> uploaded = new LinkedHashMap<>();
        student.ifPresent(s -> documentRepository.findDocumentTypesByStudentId(s.getId())
                .forEach(type -> uploaded.putIfAbsent(key(type), type)));

        Set<String> requiredNames = new LinkedHashSet<>();
        platform.stream().filter(type -> Boolean.TRUE.equals(type.getIsRequired()))
                .forEach(type -> requiredNames.add(key(type)));
        companyRequired.forEach(type -> requiredNames.add(key(type)));

        List<DocumentType> candidates = new ArrayList<>(platform);
        candidates.addAll(companyRequired);
        candidates.addAll(uploaded.values());
        Map<String, DocumentTypeResponse> byName = new LinkedHashMap<>();
        for (DocumentType type : candidates) {
            byName.computeIfAbsent(key(type), name -> {
                DocumentTypeResponse response = documentTypeMapper.toDto(uploaded.getOrDefault(name, type));
                response.setIsRequired(requiredNames.contains(name));
                return response;
            });
        }
        log.info("action=studentDocumentTypesListed studentId={} pickedBuildings={} companies={} count={}",
                student.map(Student::getId).orElse(null), picked.size(), companies, byName.size());
        return List.copyOf(byName.values());
    }

    /** The companies owning the picked buildings, then those of the student's live applications. */
    private Set<Long> companiesOf(Optional<Student> student, Set<Long> buildingIds) {
        Set<Long> companies = new LinkedHashSet<>();
        if (!buildingIds.isEmpty()) {
            buildingRepository.findAllById(buildingIds).stream()
                    .map(Building::getTenantId)
                    .forEach(companies::add);
        }
        student.ifPresent(s -> applicationRepository
                .findByStudent_IdAndStatusInOrderByIdDesc(s.getId(), ApplicationStatus.LIVE_STATUSES)
                .forEach(application -> companies.add(application.getTenantId())));
        companies.remove(null);
        companies.remove(TenantContext.PLATFORM_TENANT_ID);
        return companies;
    }

    private static String key(DocumentType type) {
        return type.getTypeName().toLowerCase(Locale.ROOT);
    }
}
