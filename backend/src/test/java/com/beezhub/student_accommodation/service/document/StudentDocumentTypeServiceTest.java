package com.beezhub.student_accommodation.service.document;

import com.beezhub.student_accommodation.mapper.DocumentTypeMapper;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GH-443: the Documents page must offer the required types of the companies a student is applying
 * to, so a company's type with a new name can be uploaded. The merged view is the real
 * {@link DocumentTypeService} over a mocked repository, so shadowing works as it does in production.
 */
@ExtendWith(MockitoExtension.class)
class StudentDocumentTypeServiceTest {

    private static final long USER_ID = 10L;
    private static final long STUDENT_ID = 20L;
    private static final long COMPANY = 5L;
    private static final long OTHER_COMPANY = 7L;
    private static final long BUILDING = 100L;

    @Mock private AuthService authService;
    @Mock private StudentRepository studentRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentTypeRepository documentTypeRepository;

    private StudentDocumentTypeService service;

    private final DocumentType nationalId = type(1L, 0L, "National ID", true);
    private final DocumentType proofOfIncome = type(2L, 0L, "Proof of Income", false);

    @BeforeEach
    void setUp() {
        DocumentTypeMapper mapper = Mappers.getMapper(DocumentTypeMapper.class);
        DocumentTypeService documentTypeService = new DocumentTypeService(documentTypeRepository, mapper, authService);
        service = new StudentDocumentTypeService(authService, studentRepository, applicationRepository,
                buildingRepository, documentRepository, documentTypeService, mapper);

        AppUser user = new AppUser();
        user.setId(USER_ID);
        user.setUserRole(UserRole.STUDENT);
        // Lenient: the over-limit case is refused before the caller is looked up.
        lenient().when(authService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getUploadableTypes_noBuildingOrApplication_returnsPlatformTypes() {
        givenStudent();
        givenTypes(0L);

        List<DocumentTypeResponse> types = service.getUploadableTypes(null);

        assertThat(types).extracting(DocumentTypeResponse::getName, DocumentTypeResponse::getIsRequired)
                .containsExactly(tuple("National ID", true), tuple("Proof of Income", false));
        verifyNoInteractions(buildingRepository);
    }

    @Test
    void getUploadableTypes_pickedBuilding_addsCompanyRequiredTypeWithNewName() {
        givenStudent();
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY, type(50L, COMPANY, "Bursary Letter", true));

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of(BUILDING));

        assertThat(types).extracting(DocumentTypeResponse::getId, DocumentTypeResponse::getName,
                        DocumentTypeResponse::getIsRequired, DocumentTypeResponse::getIsPlatformDefault)
                .containsExactly(
                        tuple("1", "National ID", true, true),
                        tuple("2", "Proof of Income", false, true),
                        tuple("50", "Bursary Letter", true, false));
    }

    @Test
    void getUploadableTypes_pickedBuilding_leavesOutCompanyOptionalTypes() {
        givenStudent();
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY, type(51L, COMPANY, "Parking Permit", false));

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of(BUILDING));

        assertThat(types).extracting(DocumentTypeResponse::getName)
                .containsExactly("National ID", "Proof of Income");
    }

    @Test
    void getUploadableTypes_companyRequiresPlatformName_keepsPlatformSlotAndMarksItRequired() {
        givenStudent();
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY, type(52L, COMPANY, "proof of income", true));

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of(BUILDING));

        assertThat(types).extracting(DocumentTypeResponse::getId, DocumentTypeResponse::getName,
                        DocumentTypeResponse::getIsRequired)
                .containsExactly(tuple("1", "National ID", true), tuple("2", "Proof of Income", true));
    }

    @Test
    void getUploadableTypes_liveApplication_addsItsCompanyRequiredTypes() {
        givenStudent();
        Application application = new Application();
        application.setTenantId(OTHER_COMPANY);
        when(applicationRepository.findByStudent_IdAndStatusInOrderByIdDesc(STUDENT_ID, ApplicationStatus.LIVE_STATUSES))
                .thenReturn(List.of(application));
        givenTypes(0L);
        givenTypes(OTHER_COMPANY, type(70L, OTHER_COMPANY, "Guarantor Form", true));

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of());

        assertThat(types).extracting(DocumentTypeResponse::getName)
                .containsExactly("National ID", "Proof of Income", "Guarantor Form");
    }

    @Test
    void getUploadableTypes_pickedBuilding_readsOnlyThatCompanysTypes() {
        givenStudent();
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY);

        service.getUploadableTypes(List.of(BUILDING));

        verify(documentTypeRepository).findAllByTenantIdIn(List.of(0L));
        verify(documentTypeRepository).findAllByTenantIdIn(List.of(0L, COMPANY));
        verify(documentTypeRepository, never()).findAllByTenantIdIn(List.of(0L, OTHER_COMPANY));
        verify(documentTypeRepository, never()).findAll();
    }

    @Test
    void getUploadableTypes_uploadedCompanyType_staysOnPageWithoutBuildingOrApplication() {
        DocumentType bursary = type(50L, COMPANY, "Bursary Letter", true);
        givenStudent(bursary);
        givenTypes(0L);

        List<DocumentTypeResponse> types = service.getUploadableTypes(null);

        assertThat(types).extracting(DocumentTypeResponse::getId, DocumentTypeResponse::getName,
                        DocumentTypeResponse::getIsRequired)
                .containsExactly(
                        tuple("1", "National ID", true),
                        tuple("2", "Proof of Income", false),
                        tuple("50", "Bursary Letter", false));
    }

    @Test
    void getUploadableTypes_uploadedUnderPlatformName_keepsTheUploadedTypeInThatSlot() {
        givenStudent(type(5L, COMPANY, "National ID", false));
        givenTypes(0L);

        List<DocumentTypeResponse> types = service.getUploadableTypes(null);

        assertThat(types).extracting(DocumentTypeResponse::getId, DocumentTypeResponse::getName,
                        DocumentTypeResponse::getIsRequired)
                .containsExactly(tuple("5", "National ID", true), tuple("2", "Proof of Income", false));
    }

    @Test
    void getUploadableTypes_unknownBuilding_isIgnored() {
        givenStudent();
        when(buildingRepository.findAllById(Set.of(999L))).thenReturn(List.of());
        givenTypes(0L);

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of(999L));

        assertThat(types).extracting(DocumentTypeResponse::getName)
                .containsExactly("National ID", "Proof of Income");
    }

    @Test
    void getUploadableTypes_moreThanThreeBuildings_throws() {
        List<Long> four = List.of(1L, 2L, 3L, 4L);

        assertThatThrownBy(() -> service.getUploadableTypes(four))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 3");
        verifyNoInteractions(buildingRepository, documentTypeRepository);
    }

    @Test
    void getUploadableTypes_repeatedBuilding_countsOnce() {
        givenStudent();
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY);

        List<Long> repeated = new ArrayList<>(Arrays.asList(BUILDING, BUILDING, BUILDING, null, BUILDING));

        assertThat(service.getUploadableTypes(repeated)).hasSize(2);
    }

    @Test
    void getUploadableTypes_noStudentProfile_returnsPlatformAndPickedCompanyTypes() {
        when(studentRepository.findByAppUser_Id(USER_ID)).thenReturn(Optional.empty());
        givenBuilding(BUILDING, COMPANY);
        givenTypes(0L);
        givenTypes(COMPANY, type(50L, COMPANY, "Bursary Letter", true));

        List<DocumentTypeResponse> types = service.getUploadableTypes(List.of(BUILDING));

        assertThat(types).extracting(DocumentTypeResponse::getName)
                .containsExactly("National ID", "Proof of Income", "Bursary Letter");
        verifyNoInteractions(documentRepository);
        verify(applicationRepository, never()).findByStudent_IdAndStatusInOrderByIdDesc(anyLong(), any());
    }

    private void givenStudent(DocumentType... uploadedTypes) {
        Student student = new Student();
        student.setId(STUDENT_ID);
        when(studentRepository.findByAppUser_Id(USER_ID)).thenReturn(Optional.of(student));
        when(documentRepository.findDocumentTypesByStudentId(STUDENT_ID)).thenReturn(List.of(uploadedTypes));
    }

    private void givenBuilding(long buildingId, long companyId) {
        Building building = new Building();
        building.setId(buildingId);
        building.setTenantId(companyId);
        when(buildingRepository.findAllById(Set.of(buildingId))).thenReturn(List.of(building));
    }

    /** The rows findAllByTenantIdIn returns for the platform, or for the platform and one company. */
    private void givenTypes(long companyId, DocumentType... companyTypes) {
        List<DocumentType> rows = new ArrayList<>(List.of(nationalId, proofOfIncome));
        rows.addAll(List.of(companyTypes));
        List<Long> tenantIds = companyId == 0L ? List.of(0L) : List.of(0L, companyId);
        when(documentTypeRepository.findAllByTenantIdIn(tenantIds)).thenReturn(rows);
    }

    private static DocumentType type(long id, long tenantId, String name, boolean required) {
        DocumentType type = new DocumentType();
        type.setId(id);
        type.setTenantId(tenantId);
        type.setTypeName(name);
        type.setIsRequired(required);
        return type;
    }
}
