package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.exceptions.UnauthorizedAccessException;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentAccessGuardTest {

    private static final long TENANT = 5L;
    private static final long OTHER_TENANT = 6L;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private DocumentAccessGuard guard;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static AppUser user(long id, UserRole role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUserRole(role);
        return user;
    }

    private static Document documentOf(AppUser owner, long studentId) {
        Student student = new Student();
        student.setId(studentId);
        student.setAppUser(owner);
        Document document = new Document();
        document.setId(1L);
        document.setStudent(student);
        return document;
    }

    @Test
    void assertCanAccessDocument_owningStudent_passes() {
        AppUser owner = user(10L, UserRole.STUDENT);

        assertThatCode(() -> guard.assertCanAccessDocument(documentOf(owner, 1L), owner))
                .doesNotThrowAnyException();
        verify(applicationRepository, never()).existsByStudent_IdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void assertCanAccessDocument_otherStudent_denied() {
        Document document = documentOf(user(10L, UserRole.STUDENT), 1L);

        assertThatThrownBy(() -> guard.assertCanAccessDocument(document, user(11L, UserRole.STUDENT)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessDocument_platformAdmin_passesWithoutTenantCheck() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        Document document = documentOf(user(10L, UserRole.STUDENT), 1L);

        assertThatCode(() -> guard.assertCanAccessDocument(document, user(2L, UserRole.PLATFORM_ADMIN)))
                .doesNotThrowAnyException();
        verify(applicationRepository, never()).existsByStudent_IdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void assertCanAccessDocument_tenantAdminWithApplicationInTenant_passes() {
        TenantContext.setTenantId(TENANT);
        when(applicationRepository.existsByStudent_IdAndTenantId(1L, TENANT)).thenReturn(true);
        Document document = documentOf(user(10L, UserRole.STUDENT), 1L);

        assertThatCode(() -> guard.assertCanAccessDocument(document, user(3L, UserRole.TENANT_ADMIN)))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessDocument_tenantAdminWithoutApplicationInTenant_denied() {
        TenantContext.setTenantId(OTHER_TENANT);
        when(applicationRepository.existsByStudent_IdAndTenantId(1L, OTHER_TENANT)).thenReturn(false);
        Document document = documentOf(user(10L, UserRole.STUDENT), 1L);

        assertThatThrownBy(() -> guard.assertCanAccessDocument(document, user(3L, UserRole.TENANT_ADMIN)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessDocumentsOfUser_ownUser_passes() {
        AppUser caller = user(10L, UserRole.STUDENT);

        assertThatCode(() -> guard.assertCanAccessDocumentsOfUser(10L, caller)).doesNotThrowAnyException();
    }

    @Test
    void assertCanAccessDocumentsOfUser_nonAdminOtherUser_denied() {
        assertThatThrownBy(() -> guard.assertCanAccessDocumentsOfUser(999L, user(10L, UserRole.STUDENT)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void assertCanAccessDocumentsOfUser_tenantAdminOutsideTenant_denied() {
        TenantContext.setTenantId(TENANT);
        when(applicationRepository.existsByStudent_AppUser_IdAndTenantId(10L, TENANT)).thenReturn(false);

        assertThatThrownBy(() -> guard.assertCanAccessDocumentsOfUser(10L, user(3L, UserRole.TENANT_ADMIN)))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void tenantScopeOf_tenantAdmin_returnsCurrentTenant() {
        TenantContext.setTenantId(TENANT);

        assertThat(guard.tenantScopeOf(user(3L, UserRole.TENANT_ADMIN))).contains(TENANT);
    }

    @Test
    void tenantScopeOf_platformAdminAndStudent_isEmpty() {
        TenantContext.setTenantId(TENANT);

        assertThat(guard.tenantScopeOf(user(2L, UserRole.PLATFORM_ADMIN))).isEmpty();
        assertThat(guard.tenantScopeOf(user(10L, UserRole.STUDENT))).isEmpty();
    }

    @Test
    void tenantScopeOf_tenantAdminWithoutTenantContext_isEmpty() {
        TenantContext.clear();

        assertThat(guard.tenantScopeOf(user(3L, UserRole.TENANT_ADMIN))).isEmpty();
    }

    @Test
    void document_isNoLongerTenantFiltered() {
        // Regression guard for GH-112: a tenant filter on Document hides every student upload,
        // because students are platform-tenant users.
        assertThat(Document.class.isAnnotationPresent(org.hibernate.annotations.Filter.class)).isFalse();
        assertThat(com.beezhub.student_accommodation.model.entity.TenantAware.class
                .isAssignableFrom(Document.class)).isFalse();
    }

    @Test
    void assertCanAccessDocument_documentWithoutStudent_deniedForTenantAdmin() {
        TenantContext.setTenantId(TENANT);
        Document detached = new Document();
        detached.setId(2L);

        assertThatThrownBy(() -> guard.assertCanAccessDocument(detached, user(3L, UserRole.TENANT_ADMIN)))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(applicationRepository, never()).existsByStudent_IdAndTenantId(any(), anyLong());
    }
}
