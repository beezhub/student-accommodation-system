package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.util.PageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeService {

    private static final Long PLATFORM_TENANT_ID = 0L;

    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentTypeMapper documentTypeMapper;
    private final AuthService authService;

    // -------------------------------------------------------------------------
    // Merged view — bypasses Hibernate filter, uses explicit IN (0, tenantId)
    // -------------------------------------------------------------------------

    /**
     * Returns the merged view of document types for a given tenant:
     * platform types (tenant_id=0) + tenant's own types.
     * When a tenant type has the same type_name as a platform type, the tenant row wins.
     *
     * <p>This merge is why {@code DocumentType} deliberately carries no {@code tenantFilter}
     * despite having a {@code tenant_id} column (GH-126): the filter would hide the platform
     * defaults from every tenant admin and leave them with an empty or partial checklist, the same
     * trap that made student uploads invisible in GH-112. Cross-tenant access is refused
     * explicitly in {@link #guardPlatformType} instead.
     */
    public List<DocumentType> getEffectiveDocumentTypes(Long tenantId) {
        List<Long> tenantIds = tenantId.equals(PLATFORM_TENANT_ID)
                ? List.of(PLATFORM_TENANT_ID)
                : List.of(PLATFORM_TENANT_ID, tenantId);

        List<DocumentType> all = documentTypeRepository.findAllByTenantIdIn(tenantIds);

        // Deduplicate: tenant row wins over platform row with same type_name
        Map<String, DocumentType> byName = new LinkedHashMap<>();
        // Add platform types first, then tenant types override by name
        all.stream()
                .filter(dt -> dt.getTenantId().equals(PLATFORM_TENANT_ID))
                .forEach(dt -> byName.put(dt.getTypeName().toLowerCase(), dt));
        all.stream()
                .filter(dt -> !dt.getTenantId().equals(PLATFORM_TENANT_ID))
                .forEach(dt -> byName.put(dt.getTypeName().toLowerCase(), dt));

        return List.copyOf(byName.values());
    }

    /**
     * Returns the set of required type names for a given tenant (merged view, isRequired=true).
     * Used for name-based completeness checks at application submission and allocation gate.
     */
    public Set<String> getEffectiveRequiredTypeNames(Long tenantId) {
        return getEffectiveDocumentTypes(tenantId).stream()
                .filter(DocumentType::getIsRequired)
                .map(dt -> dt.getTypeName().toLowerCase())
                .collect(Collectors.toSet());
    }

    /**
     * The names of a company's expiring document types (merged view, lower case), matched by name
     * like {@link #getEffectiveRequiredTypeNames} (GH-498).
     */
    public Set<String> getEffectiveExpiringTypeNames(Long tenantId) {
        return getEffectiveDocumentTypes(tenantId).stream()
                .filter(dt -> Boolean.TRUE.equals(dt.getIsExpiring()))
                .map(dt -> dt.getTypeName().toLowerCase())
                .collect(Collectors.toSet());
    }

    // -------------------------------------------------------------------------
    // Public-facing list/search methods
    // -------------------------------------------------------------------------

    public List<DocumentTypeResponse> getAllDocumentTypes() {
        Long tenantId = resolveTenantId();
        log.info("Fetching all document types for tenantId={}", tenantId);
        List<DocumentTypeResponse> documentTypes = getEffectiveDocumentTypes(tenantId).stream()
                .map(documentTypeMapper::toDto)
                .collect(Collectors.toList());
        log.info("Found {} document types", documentTypes.size());
        return documentTypes;
    }

    public PageResponse<DocumentTypeResponse> getAllDocumentTypes(Pageable pageable) {
        Long tenantId = resolveTenantId();
        log.info("Fetching paginated document types for tenantId={}", tenantId);
        List<DocumentTypeResponse> all = getEffectiveDocumentTypes(tenantId).stream()
                .map(documentTypeMapper::toDto)
                .collect(Collectors.toList());
        return PageMapper.toPageResponse(toPage(all, pageable));
    }

    public PageResponse<DocumentTypeResponse> searchDocumentTypes(String name, Boolean isRequired, Pageable pageable) {
        Long tenantId = resolveTenantId();
        log.info("Searching document types - name: {}, isRequired: {}, tenantId: {}", name, isRequired, tenantId);
        List<DocumentTypeResponse> filtered = getEffectiveDocumentTypes(tenantId).stream()
                .filter(dt -> name == null || dt.getTypeName().toLowerCase().contains(name.toLowerCase()))
                .filter(dt -> isRequired == null || isRequired.equals(dt.getIsRequired()))
                .map(documentTypeMapper::toDto)
                .collect(Collectors.toList());
        return PageMapper.toPageResponse(toPage(filtered, pageable));
    }

    private <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> content = start >= list.size() ? List.of() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    /**
     * Returns required document types for the given tenantId (merged view).
     * When tenantId is null, falls back to platform defaults only.
     */
    public List<DocumentTypeResponse> getRequiredDocumentTypes(Long tenantId) {
        Long effectiveTenantId = tenantId != null ? tenantId : PLATFORM_TENANT_ID;
        log.info("Fetching required document types for tenantId={}", effectiveTenantId);
        List<DocumentTypeResponse> required = getEffectiveDocumentTypes(effectiveTenantId).stream()
                .filter(DocumentType::getIsRequired)
                .map(documentTypeMapper::toDto)
                .collect(Collectors.toList());
        log.info("Found {} required document types", required.size());
        return required;
    }

    public DocumentTypeResponse getDocumentTypeById(Long id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        return documentTypeMapper.toDto(documentType);
    }

    // -------------------------------------------------------------------------
    // Mutations — with platform guard and tenant stamping
    // -------------------------------------------------------------------------

    public DocumentTypeResponse createDocumentType(DocumentTypeRequest request) {
        DocumentType documentType = new DocumentType();
        documentType.setTypeName(request.getName());
        documentType.setDescription(request.getDescription());
        documentType.setIsRequired(request.getIsRequired());
        documentType.setIsExpiring(Boolean.TRUE.equals(request.getIsExpiring()));
        documentType.setTenantId(resolveWriteTenantId());
        DocumentType saved = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(saved);
    }

    public DocumentTypeResponse updateDocumentType(Long id, DocumentTypeRequest request) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        guardPlatformType(documentType);
        documentType.setTypeName(request.getName());
        documentType.setDescription(request.getDescription());
        documentType.setIsRequired(request.getIsRequired());
        if (request.getIsExpiring() != null) {
            documentType.setIsExpiring(request.getIsExpiring());
        }
        DocumentType updated = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(updated);
    }

    /** Marks a type as expiring with the academic year it was uploaded in, or not (GH-498). */
    public DocumentTypeResponse updateExpiringFlag(Long id, boolean expiring) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        guardPlatformType(documentType);
        documentType.setIsExpiring(expiring);
        return documentTypeMapper.toDto(documentTypeRepository.save(documentType));
    }

    public DocumentTypeResponse updateRequiredFlag(Long id, boolean required) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        guardPlatformType(documentType);
        documentType.setIsRequired(required);
        DocumentType updated = documentTypeRepository.save(documentType);
        return documentTypeMapper.toDto(updated);
    }

    public void deleteDocumentType(Long id) {
        DocumentType documentType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new DocumentTypeNotFoundException("Document type not found"));
        guardPlatformType(documentType);
        documentTypeRepository.delete(documentType);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : PLATFORM_TENANT_ID;
    }

    /** Returns the tenant_id to stamp on newly created document types. */
    private Long resolveWriteTenantId() {
        UserRole role = authService.getCurrentUser().getUserRole();
        return role == UserRole.PLATFORM_ADMIN ? PLATFORM_TENANT_ID : resolveTenantId();
    }

    /**
     * Throws 403 if a TENANT_ADMIN tries to mutate a platform type (tenant_id=0).
     * PLATFORM_ADMIN may always mutate platform types.
     */
    private void guardPlatformType(DocumentType documentType) {
        if (PLATFORM_TENANT_ID.equals(documentType.getTenantId())) {
            UserRole role = authService.getCurrentUser().getUserRole();
            if (role != UserRole.PLATFORM_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tenant admins cannot modify platform-level document types");
            }
            return;
        }
        // DocumentType is not TenantAware, so no Hibernate filter ever applied and findById would
        // return another tenant's row. Reject it here (GH-111).
        Long callerTenantId = TenantContext.getTenantId();
        if (callerTenantId != null
                && !PLATFORM_TENANT_ID.equals(callerTenantId)
                && !callerTenantId.equals(documentType.getTenantId())) {
            log.warn("action=crossTenantAccessDenied entity=DocumentType id={} ownerTenantId={} callerTenantId={}",
                    documentType.getId(), documentType.getTenantId(), callerTenantId);
            throw new DocumentTypeNotFoundException("Document type not found");
        }
    }
}
