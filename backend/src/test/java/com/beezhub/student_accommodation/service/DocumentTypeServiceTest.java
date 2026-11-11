package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DocumentTypeNotFoundException;
import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeRequest;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private DocumentTypeMapper documentTypeMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private DocumentTypeService documentTypeService;

    private DocumentType platformType1;   // tenant_id=0, isRequired=true
    private DocumentType platformType2;   // tenant_id=0, isRequired=false
    private DocumentType tenantType;      // tenant_id=42, isRequired=true (shadows platformType1 name)
    private DocumentTypeResponse response1;
    private DocumentTypeResponse response2;
    private DocumentTypeRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        platformType1 = new DocumentType();
        platformType1.setId(1L);
        platformType1.setTenantId(0L);
        platformType1.setTypeName("ID Document");
        platformType1.setDescription("Government issued ID");
        platformType1.setIsRequired(true);

        platformType2 = new DocumentType();
        platformType2.setId(2L);
        platformType2.setTenantId(0L);
        platformType2.setTypeName("Proof of Income");
        platformType2.setDescription("Income verification");
        platformType2.setIsRequired(false);

        tenantType = new DocumentType();
        tenantType.setId(5L);
        tenantType.setTenantId(42L);
        tenantType.setTypeName("ID Document"); // same name as platformType1 — shadow
        tenantType.setDescription("Tenant-specific ID check");
        tenantType.setIsRequired(false); // overrides platform's isRequired=true

        response1 = new DocumentTypeResponse();
        response1.setId("1");
        response1.setName("ID Document");
        response1.setDescription("Government issued ID");
        response1.setIsRequired(true);

        response2 = new DocumentTypeResponse();
        response2.setId("2");
        response2.setName("Proof of Income");
        response2.setDescription("Income verification");
        response2.setIsRequired(false);

        request = new DocumentTypeRequest();
        request.setName("New Document");
        request.setDescription("New description");
        request.setIsRequired(true);

        pageable = PageRequest.of(0, 20);
    }

    // -------------------------------------------------------------------------
    // getEffectiveDocumentTypes — merged view + shadow semantics
    // -------------------------------------------------------------------------

    @Test
    void getEffectiveDocumentTypes_PlatformOnly_ReturnsPlatformTypes() {
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L)))
                .thenReturn(List.of(platformType1, platformType2));

        List<DocumentType> result = documentTypeService.getEffectiveDocumentTypes(0L);

        assertEquals(2, result.size());
        verify(documentTypeRepository).findAllByTenantIdIn(List.of(0L));
    }

    @Test
    void getEffectiveDocumentTypes_TenantShadowWinsOverPlatformRow() {
        // tenantType has same typeName as platformType1 — tenant row should win
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L, 42L)))
                .thenReturn(List.of(platformType1, platformType2, tenantType));

        List<DocumentType> result = documentTypeService.getEffectiveDocumentTypes(42L);

        // Should have 2 entries: tenantType (shadow) and platformType2
        assertEquals(2, result.size());
        // The "ID Document" entry must be the tenant shadow (id=5), not platform (id=1)
        assertTrue(result.stream()
                .filter(dt -> dt.getTypeName().equalsIgnoreCase("ID Document"))
                .allMatch(dt -> dt.getId().equals(5L)));
    }

    @Test
    void getEffectiveDocumentTypes_TenantOnlyTypeAdded() {
        DocumentType tenantOnly = new DocumentType();
        tenantOnly.setId(10L);
        tenantOnly.setTenantId(42L);
        tenantOnly.setTypeName("Lease Agreement");
        tenantOnly.setIsRequired(true);

        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L, 42L)))
                .thenReturn(List.of(platformType1, platformType2, tenantOnly));

        List<DocumentType> result = documentTypeService.getEffectiveDocumentTypes(42L);

        assertEquals(3, result.size());
    }

    // -------------------------------------------------------------------------
    // getEffectiveRequiredTypeNames — name-based required type set
    // -------------------------------------------------------------------------

    @Test
    void getEffectiveRequiredTypeNames_ReturnsPlatformRequired() {
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L)))
                .thenReturn(List.of(platformType1, platformType2));

        Set<String> names = documentTypeService.getEffectiveRequiredTypeNames(0L);

        assertEquals(Set.of("id document"), names);
    }

    @Test
    void getEffectiveRequiredTypeNames_TenantShadowMakesRequiredTypeOptional() {
        // tenantType shadows platformType1 "ID Document" with isRequired=false
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L, 42L)))
                .thenReturn(List.of(platformType1, platformType2, tenantType));

        Set<String> names = documentTypeService.getEffectiveRequiredTypeNames(42L);

        // "ID Document" is no longer required because the tenant shadow says isRequired=false
        assertTrue(names.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getRequiredDocumentTypes — with tenantId param
    // -------------------------------------------------------------------------

    @Test
    void getRequiredDocumentTypes_WithNullTenantId_DefaultsToPlatform() {
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L)))
                .thenReturn(List.of(platformType1, platformType2));
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        List<DocumentTypeResponse> result = documentTypeService.getRequiredDocumentTypes(null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsRequired());
    }

    // -------------------------------------------------------------------------
    // getAllDocumentTypes (pageable) — unchanged behaviour
    // -------------------------------------------------------------------------

    @Test
    void getAllDocumentTypes_WithPageable_ShouldReturnPageResponse() {
        // Paged listing is backed by the tenant-aware merged view (no TenantContext
        // in unit tests -> resolves to the platform tenant 0).
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L)))
                .thenReturn(List.of(platformType1, platformType2));
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);
        when(documentTypeMapper.toDto(platformType2)).thenReturn(response2);

        PageResponse<DocumentTypeResponse> result = documentTypeService.getAllDocumentTypes(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(documentTypeRepository).findAllByTenantIdIn(List.of(0L));
    }

    @Test
    void searchDocumentTypes_WithNameFilter_ShouldReturnFilteredResults() {
        // Search filters the tenant-aware merged view in memory.
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L)))
                .thenReturn(List.of(platformType1, platformType2));
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        PageResponse<DocumentTypeResponse> result = documentTypeService.searchDocumentTypes("ID", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(documentTypeRepository).findAllByTenantIdIn(List.of(0L));
    }

    // -------------------------------------------------------------------------
    // getDocumentTypeById
    // -------------------------------------------------------------------------

    @Test
    void getDocumentTypeById_ShouldReturnDocumentType() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(platformType1));
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        DocumentTypeResponse result = documentTypeService.getDocumentTypeById(1L);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    void getDocumentTypeById_NotFound_ShouldThrowException() {
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(DocumentTypeNotFoundException.class, () -> documentTypeService.getDocumentTypeById(1L));
    }

    // -------------------------------------------------------------------------
    // createDocumentType — tenant stamping
    // -------------------------------------------------------------------------

    @Test
    void createDocumentType_AsTenantAdmin_StampsTenantId() {
        AppUser tenantAdmin = new AppUser();
        tenantAdmin.setUserRole(UserRole.TENANT_ADMIN);
        when(authService.getCurrentUser()).thenReturn(tenantAdmin);
        when(documentTypeRepository.save(any(DocumentType.class))).thenReturn(platformType1);
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        documentTypeService.createDocumentType(request);

        verify(documentTypeRepository).save(any(DocumentType.class));
    }

    @Test
    void createDocumentType_AsPlatformAdmin_StampsTenantIdZero() {
        AppUser platformAdmin = new AppUser();
        platformAdmin.setUserRole(UserRole.PLATFORM_ADMIN);
        when(authService.getCurrentUser()).thenReturn(platformAdmin);
        when(documentTypeRepository.save(any(DocumentType.class))).thenAnswer(inv -> {
            DocumentType dt = inv.getArgument(0);
            assertEquals(0L, dt.getTenantId());
            return platformType1;
        });
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        documentTypeService.createDocumentType(request);

        verify(documentTypeRepository).save(any(DocumentType.class));
    }

    // -------------------------------------------------------------------------
    // Mutation guards — TENANT_ADMIN cannot mutate platform types
    // -------------------------------------------------------------------------

    @Test
    void updateDocumentType_TenantAdminOnPlatformType_ThrowsForbidden() {
        AppUser tenantAdmin = new AppUser();
        tenantAdmin.setUserRole(UserRole.TENANT_ADMIN);
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(platformType1));
        when(authService.getCurrentUser()).thenReturn(tenantAdmin);

        assertThrows(ResponseStatusException.class,
                () -> documentTypeService.updateDocumentType(1L, request));
    }

    @Test
    void deleteDocumentType_TenantAdminOnPlatformType_ThrowsForbidden() {
        AppUser tenantAdmin = new AppUser();
        tenantAdmin.setUserRole(UserRole.TENANT_ADMIN);
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(platformType1));
        when(authService.getCurrentUser()).thenReturn(tenantAdmin);

        assertThrows(ResponseStatusException.class,
                () -> documentTypeService.deleteDocumentType(1L));
    }

    @Test
    void updateDocumentType_PlatformAdminOnPlatformType_Succeeds() {
        AppUser platformAdmin = new AppUser();
        platformAdmin.setUserRole(UserRole.PLATFORM_ADMIN);
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(platformType1));
        when(authService.getCurrentUser()).thenReturn(platformAdmin);
        when(documentTypeRepository.save(platformType1)).thenReturn(platformType1);
        when(documentTypeMapper.toDto(platformType1)).thenReturn(response1);

        assertDoesNotThrow(() -> documentTypeService.updateDocumentType(1L, request));
        verify(documentTypeRepository).save(platformType1);
    }

    @Test
    void deleteDocumentType_TenantAdminOnOwnType_Succeeds() {
        // tenantType has tenantId=42 (not 0), so guardPlatformType skips the auth check
        when(documentTypeRepository.findById(5L)).thenReturn(Optional.of(tenantType));

        assertDoesNotThrow(() -> documentTypeService.deleteDocumentType(5L));
        verify(documentTypeRepository).delete(tenantType);
    }

    // -------------------------------------------------------------------------
    // Expiring types (GH-498)
    // -------------------------------------------------------------------------

    @Test
    void getEffectiveExpiringTypeNames_tenantShadowDecides() {
        platformType1.setIsExpiring(false);
        tenantType.setIsExpiring(true);
        platformType2.setIsExpiring(true);
        when(documentTypeRepository.findAllByTenantIdIn(List.of(0L, 42L)))
                .thenReturn(List.of(platformType1, platformType2, tenantType));

        assertEquals(java.util.Set.of("id document", "proof of income"),
                documentTypeService.getEffectiveExpiringTypeNames(42L));
    }

    @Test
    void updateExpiringFlag_companyAdminOnOwnType_switchesItOn() {
        when(documentTypeRepository.findById(5L)).thenReturn(Optional.of(tenantType));
        when(documentTypeRepository.save(tenantType)).thenReturn(tenantType);

        documentTypeService.updateExpiringFlag(5L, true);

        assertTrue(tenantType.getIsExpiring());
    }

    @Test
    void updateExpiringFlag_tenantAdminOnPlatformType_throwsForbidden() {
        AppUser tenantAdmin = new AppUser();
        tenantAdmin.setUserRole(UserRole.TENANT_ADMIN);
        when(documentTypeRepository.findById(1L)).thenReturn(Optional.of(platformType1));
        when(authService.getCurrentUser()).thenReturn(tenantAdmin);

        assertThrows(ResponseStatusException.class, () -> documentTypeService.updateExpiringFlag(1L, true));
    }

    @Test
    void createAndUpdate_carryTheExpiringFlag_andAnUpdateWithoutItKeepsIt() {
        AppUser platformAdmin = new AppUser();
        platformAdmin.setUserRole(UserRole.PLATFORM_ADMIN);
        when(authService.getCurrentUser()).thenReturn(platformAdmin);
        when(documentTypeRepository.save(any(DocumentType.class))).thenAnswer(inv -> inv.getArgument(0));
        request.setIsExpiring(true);

        documentTypeService.createDocumentType(request);
        org.mockito.ArgumentCaptor<DocumentType> created = org.mockito.ArgumentCaptor.forClass(DocumentType.class);
        verify(documentTypeRepository).save(created.capture());
        assertTrue(created.getValue().getIsExpiring());

        tenantType.setIsExpiring(true);
        when(documentTypeRepository.findById(5L)).thenReturn(Optional.of(tenantType));
        request.setIsExpiring(null);
        documentTypeService.updateDocumentType(5L, request);
        assertTrue(tenantType.getIsExpiring());
    }
}
