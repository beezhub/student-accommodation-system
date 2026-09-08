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
 * document only when its student has an application in that admin's tenant. Platform-level admins
 * (PLATFORM_ADMIN, PLATFORM_STAFF) see everything; students see their own documents.
 *
 * <p>Both entry points gate on {@code UserRole.isAdmin()} before any scoping happens, so a role
 * left out of that predicate is not merely unscoped here — it is denied documents outright.
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
     * (a platform-level role) or is not an admin at all. Callers use it to narrow a search
     * specification.
     *
     * <p>The first branch asks {@code isPlatformLevel()} rather than naming PLATFORM_ADMIN. Both
     * spellings return an empty scope for PLATFORM_STAFF today, but for different reasons: named,
     * staff would fall through to the tenant check and get an empty scope only because platform
     * accounts happen to be stamped tenant 0 (GH-238). That is the right answer reached by
     * accident, and it would stop being reached the moment a platform account carried a real
     * tenant id. Asking the role directly makes the cross-tenant view a property of the role.
     */
    public Optional<Long> tenantScopeOf(AppUser caller) {
        UserRole role = caller.getUserRole();
        if (role == null || role.isPlatformLevel() || !role.isAdmin()) {
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
