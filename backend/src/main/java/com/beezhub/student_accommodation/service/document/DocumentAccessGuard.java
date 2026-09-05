package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Tenant scoping for student documents.
 *
 * <p>Documents are student-owned and carry no Hibernate tenant filter (see {@link Document}), so
 * the scope a tenant admin gets must be applied explicitly here: a non-platform admin may reach a
 * document only when its student has an application in that admin's tenant. Platform admins see
 * everything; students see their own documents.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAccessGuard {

    private final ApplicationRepository applicationRepository;

    /** Throws unless the caller owns the document or is an admin entitled to the student's tenant. */
    public void assertCanAccessDocument(Document document, AppUser caller) {
        if (owns(document, caller)) {
            return;
        }
        if (!caller.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to document");
        }
        assertAdminCoversStudent(document.getStudent() == null ? null : document.getStudent().getId(), caller);
    }

    /** Throws unless the caller is the given user, a platform admin, or a tenant admin covering that student. */
    public void assertCanAccessDocumentsOfUser(Long ownerUserId, AppUser caller) {
        if (caller.getId().equals(ownerUserId)) {
            return;
        }
        if (!caller.getUserRole().isAdmin()) {
            throw new UnauthorizedAccessException("Unauthorized access to documents for user ID: " + ownerUserId);
        }
        if (tenantScopeOf(caller).isEmpty()) {
            return;
        }
        boolean covered = applicationRepository.existsByStudent_AppUser_IdAndTenantId(
                ownerUserId, tenantScopeOf(caller).get());
        if (!covered) {
            deny(ownerUserId, caller);
        }
    }

    /**
     * The tenant an admin's document view is limited to, or empty when the caller sees every tenant
     * (platform admin) or is not an admin at all. Callers use it to narrow a search specification.
     */
    public Optional<Long> tenantScopeOf(AppUser caller) {
        UserRole role = caller.getUserRole();
        if (role == null || role == UserRole.PLATFORM_ADMIN || !role.isAdmin()) {
            return Optional.empty();
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || TenantContext.PLATFORM_TENANT_ID.equals(tenantId)) {
            return Optional.empty();
        }
        return Optional.of(tenantId);
    }

    private void assertAdminCoversStudent(Long studentId, AppUser caller) {
        Optional<Long> scope = tenantScopeOf(caller);
        if (scope.isEmpty()) {
            return;
        }
        if (studentId == null || !applicationRepository.existsByStudent_IdAndTenantId(studentId, scope.get())) {
            deny(studentId, caller);
        }
    }

    private static boolean owns(Document document, AppUser caller) {
        return document.getStudent() != null
                && document.getStudent().getAppUser() != null
                && document.getStudent().getAppUser().equals(caller);
    }

    private static void deny(Long subjectId, AppUser caller) {
        log.warn("action=documentAccessDenied subjectId={} callerUserId={} tenantId={}",
                subjectId, caller.getId(), TenantContext.getTenantId());
        throw new UnauthorizedAccessException("Unauthorized access to document");
    }
}
