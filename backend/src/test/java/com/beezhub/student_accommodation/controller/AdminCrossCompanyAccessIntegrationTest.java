package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.AbstractIntegrationTest;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.AllocationAuditLog;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentComment;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.entity.Tenant;
import com.beezhub.student_accommodation.model.enums.AllocationAuditActionType;
import com.beezhub.student_accommodation.model.enums.AllocationStatus;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AllocationAuditLogRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentCommentRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.repository.TenantRepository;
import com.beezhub.student_accommodation.security.jwt.JwtUtil;
import com.beezhub.student_accommodation.security.user.AppUserDetails;
import com.beezhub.student_accommodation.service.allocation.AllocationLifecycleService;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may change or read a company's rows through an id in the request (GH-449).
 *
 * <p>Hibernate's tenant filter does not apply to {@code findById}, so a tenant admin reached another
 * company's rows by changing a number. Every operation here is checked in both directions: refused
 * for a tenant admin of company A on company B's row, with the row left as it was, and still working
 * for that admin on their own rows and for platform roles on any company's rows.
 *
 * <p>Application, room and allocation refusals are 404 ({@code TenantOwnershipGuard}). Document
 * refusals are 403 ({@code DocumentAccessGuard}), because documents are not tenant-scoped.
 */
@DisplayName("Admin cross-company access (integration)")
class AdminCrossCompanyAccessIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_REVIEW_ROUTE = "/api/admin/applications/%d/save-review-progress";
    private static final String REVIEW_ROUTE = "/api/applications/%d/save-review-progress";
    private static final String ENQUEUE_ROUTE = "/api/admin/allocation/waiting-list/enqueue";

    /** Whoever is allowed in, paired by {@link #allowed(Caller)} with a company they may work on. */
    enum Caller { TENANT_ADMIN_ON_OWN_COMPANY, PLATFORM_ADMIN, PLATFORM_STAFF }

    /** One company's rows, each usable by exactly one test. */
    private record Company(Tenant tenant, Building building, Room room, Room spareRoom, RoomAttributes attributes,
                           Student applicant, Application application, Document document, DocumentComment comment,
                           Student resident, RoomAllocation activeAllocation, AppUser admin, String adminToken) {
    }

    private record Access(String token, Company company) {
    }

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwtUtil;
    @Autowired AppUserRepository appUserRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired RoomAttributesRepository roomAttributesRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired RoomAllocationRepository roomAllocationRepository;
    @Autowired AllocationAuditLogRepository allocationAuditLogRepository;
    @Autowired DocumentRepository documentRepository;
    @Autowired DocumentTypeRepository documentTypeRepository;
    @Autowired DocumentCommentRepository documentCommentRepository;
    @Autowired AllocationLifecycleService allocationLifecycleService;

    private final List<Long> tenantIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> studentIds = new ArrayList<>();
    private final List<Long> documentTypeIds = new ArrayList<>();

    private Company own;
    private Company other;
    private String platformAdminToken;
    private String platformStaffToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        own = seedCompany("Own");
        other = seedCompany("Other");
        platformAdminToken = tokenFor(saveUser(UserRole.PLATFORM_ADMIN, TenantContext.PLATFORM_TENANT_ID));
        platformStaffToken = tokenFor(saveUser(UserRole.PLATFORM_STAFF, TenantContext.PLATFORM_TENANT_ID));
        TenantContext.clear();
    }

    /**
     * Native SQL because the rows hang off each other through many foreign keys. Retried because the
     * notification listeners run after commit on another thread and can still be inserting rows.
     */
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        Awaitility.await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500))
                .ignoreExceptions()
                .untilAsserted(this::deleteSeededRows);
    }

    private void deleteSeededRows() {
        String tenants = ids(tenantIds);
        String users = ids(userIds);
        String students = ids(studentIds);
        String allocations = "(SELECT id FROM room_allocation WHERE student_id IN (" + students + ") OR tenant_id IN ("
                + tenants + "))";
        String documents = "(SELECT id FROM document WHERE student_id IN (" + students + "))";
        for (String sql : List.of(
                "DELETE FROM allocation_audit_log WHERE allocation_id IN " + allocations,
                "DELETE FROM document_comments WHERE document_id IN " + documents,
                "DELETE FROM document WHERE student_id IN (" + students + ")",
                "DELETE FROM document_type WHERE id IN (" + ids(documentTypeIds) + ")",
                "DELETE FROM user_notifications WHERE user_id IN (" + users + ")",
                "DELETE FROM notification WHERE user_id IN (" + users + ") OR tenant_id IN (" + tenants + ")",
                "DELETE FROM activity_logs WHERE user_id IN (" + users + ") OR tenant_id IN (" + tenants + ")",
                "DELETE FROM room_allocation WHERE student_id IN (" + students + ") OR tenant_id IN (" + tenants + ")",
                "DELETE FROM waiting_list_entry WHERE student_id IN (" + students + ")",
                "DELETE FROM application WHERE student_id IN (" + students + ")",
                "DELETE FROM room_attributes WHERE room_id IN (SELECT id FROM room WHERE tenant_id IN (" + tenants + "))",
                "DELETE FROM room WHERE tenant_id IN (" + tenants + ")",
                "DELETE FROM building WHERE tenant_id IN (" + tenants + ")",
                "DELETE FROM student WHERE id IN (" + students + ")",
                "DELETE FROM app_user WHERE id IN (" + users + ")",
                "DELETE FROM tenant WHERE id IN (" + tenants + ")")) {
            jdbc.update(sql);
        }
    }

    // ==================== Save review progress ====================

    @ParameterizedTest
    @ValueSource(strings = {ADMIN_REVIEW_ROUTE, REVIEW_ROUTE})
    void saveReviewProgress_otherCompanyApplication_returnsNotFoundAndLeavesDocumentUnchanged(String route) {
        ResponseEntity<String> response = send(HttpMethod.POST, route.formatted(other.application().getId()),
                reviewBody(other.document().getId()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(documentStatus(other.document())).isEqualTo("PENDING");
        assertThat(commentCount(other.document())).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void saveReviewProgress_allowedCaller_updatesDocument(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.POST,
                ADMIN_REVIEW_ROUTE.formatted(company.application().getId()),
                reviewBody(company.document().getId()), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(documentStatus(company.document())).isEqualTo("APPROVED");
    }

    // ==================== Move ====================

    @Test
    void moveStudent_otherCompanyRoom_returnsNotFoundAndLeavesAllocationInPlace() {
        ResponseEntity<String> response = send(HttpMethod.POST, moveRoute(own.activeAllocation()),
                moveBody(other.spareRoom()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocatedRoomId(own.activeAllocation())).isEqualTo(own.room().getId());
    }

    @Test
    void moveStudent_otherCompanyAllocation_returnsNotFoundAndLeavesAllocationInPlace() {
        ResponseEntity<String> response = send(HttpMethod.POST, moveRoute(other.activeAllocation()),
                moveBody(own.spareRoom()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocatedRoomId(other.activeAllocation())).isEqualTo(other.room().getId());
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void moveStudent_allowedCaller_movesAllocation(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.POST, moveRoute(company.activeAllocation()),
                moveBody(company.spareRoom()), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allocatedRoomId(company.activeAllocation())).isEqualTo(company.spareRoom().getId());
    }

    // ==================== Create and update an offer ====================

    @Test
    void createRoomAllocation_otherCompanyRoom_returnsNotFoundAndCreatesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation",
                offerBody(own.applicant(), other.room(), own.application()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocationCount(own.applicant())).isZero();
    }

    @Test
    void createRoomAllocation_otherCompanyApplication_returnsNotFoundAndCreatesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation",
                offerBody(other.applicant(), own.room(), other.application()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocationCount(other.applicant())).isZero();
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void createRoomAllocation_allowedCaller_createsOffer(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation",
                offerBody(company.applicant(), company.room(), company.application()), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(allocationCount(company.applicant())).isEqualTo(1);
    }

    /**
     * The batch job allocates under the tenant it derives from the building, because applications
     * created by a platform admin are stamped with the platform tenant (GH-146). Refusing those
     * would silently cost the student their place in the queue.
     */
    @Test
    void createRoomAllocation_platformStampedApplicationUnderOwningCompany_createsOffer() {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        Application platformStamped = saveApplication(saveStudent(UserRole.STUDENT),
                TenantContext.PLATFORM_TENANT_ID, ApplicationStatus.APPROVED);
        TenantContext.setTenantId(own.tenant().getId());
        RoomAllocationRequest request = new RoomAllocationRequest();
        request.setStudentId(platformStamped.getStudent().getId());
        request.setRoomId(own.room().getId());
        request.setApplicationId(platformStamped.getId());

        allocationLifecycleService.createRoomAllocation(request);

        assertThat(allocationCount(platformStamped.getStudent())).isEqualTo(1);
    }

    @Test
    void allocateRoomByApplication_otherCompanyApplication_returnsNotFoundAndCreatesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation/allocate-by-application",
                allocateBody(other.application(), own.room()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocationCount(other.applicant())).isZero();
    }

    @Test
    void allocateRoomByApplication_otherCompanyRoom_returnsNotFoundAndCreatesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation/allocate-by-application",
                allocateBody(own.application(), other.room()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocationCount(own.applicant())).isZero();
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void allocateRoomByApplication_allowedCaller_createsOffer(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.POST, "/api/room-allocation/allocate-by-application",
                allocateBody(company.application(), company.room()), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(allocationCount(company.applicant())).isEqualTo(1);
    }

    @Test
    void updateRoomAllocation_otherCompanyRoom_returnsNotFoundAndLeavesOfferInPlace() {
        RoomAllocation offer = saveOffer(own);

        ResponseEntity<String> response = send(HttpMethod.PUT, "/api/room-allocation/" + offer.getId(),
                offerBody(offer.getStudent(), other.spareRoom(), null), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(allocatedRoomId(offer)).isEqualTo(own.room().getId());
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void updateRoomAllocation_allowedCaller_movesOffer(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();
        RoomAllocation offer = saveOffer(company);

        ResponseEntity<String> response = send(HttpMethod.PUT, "/api/room-allocation/" + offer.getId(),
                offerBody(offer.getStudent(), company.spareRoom(), null), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(allocatedRoomId(offer)).isEqualTo(company.spareRoom().getId());
    }

    // ==================== Room attributes ====================

    @Test
    void saveAttributes_otherCompanyRoom_returnsNotFoundAndLeavesAttributesUnchanged() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/admin/room-attributes",
                attributesBody(other.room()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(minYearOfStudy(other.room())).isEqualTo(1);
    }

    @Test
    void bulkSaveAttributes_otherCompanyRoomAmongOwn_returnsNotFoundAndSavesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, "/api/admin/room-attributes/bulk",
                "[" + attributesBody(own.room()) + "," + attributesBody(other.room()) + "]", own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(minYearOfStudy(other.room())).isEqualTo(1);
        assertThat(minYearOfStudy(own.room())).isEqualTo(1);
    }

    @Test
    void deleteAttributes_otherCompanyRoom_returnsNotFoundAndKeepsAttributes() {
        ResponseEntity<String> response = send(HttpMethod.DELETE,
                "/api/admin/room-attributes/room/" + other.room().getId(), null, own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(attributesCount(other.room())).isEqualTo(1);
    }

    @Test
    void getAttributesByRoomId_otherCompanyRoom_returnsNotFound() {
        ResponseEntity<String> response = send(HttpMethod.GET,
                "/api/admin/room-attributes/room/" + other.room().getId(), null, own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAttributesByBuildingId_otherCompanyBuilding_returnsNotFoundAndHidesRooms() {
        ResponseEntity<String> response = send(HttpMethod.GET,
                "/api/admin/room-attributes/building/" + other.building().getId(), null, own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).doesNotContain(other.room().getRoomNumber());
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void roomAttributes_allowedCaller_canReadSaveAndDelete(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();
        Room room = company.room();

        assertThat(send(HttpMethod.GET, "/api/admin/room-attributes/room/" + room.getId(), null, access.token())
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(send(HttpMethod.GET, "/api/admin/room-attributes/building/" + company.building().getId(), null,
                access.token()).getBody()).contains(room.getRoomNumber());

        assertThat(send(HttpMethod.POST, "/api/admin/room-attributes", attributesBody(room), access.token())
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(minYearOfStudy(room)).isEqualTo(2);

        assertThat(send(HttpMethod.POST, "/api/admin/room-attributes/bulk",
                "[" + attributesBody(company.spareRoom()) + "]", access.token()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(minYearOfStudy(company.spareRoom())).isEqualTo(2);

        assertThat(send(HttpMethod.DELETE, "/api/admin/room-attributes/room/" + room.getId(), null, access.token())
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(attributesCount(room)).isZero();
    }

    // ==================== Document comments ====================

    @Test
    void addComment_otherCompanyDocument_returnsForbiddenAndAddsNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, commentsRoute(other.document()), commentBody(),
                own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(commentCount(other.document())).isEqualTo(1);
    }

    @Test
    void getCommentsByDocument_otherCompanyDocument_returnsForbidden() {
        ResponseEntity<String> response = send(HttpMethod.GET, commentsRoute(other.document()), null,
                own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).doesNotContain(other.comment().getCommentText());
    }

    @Test
    void deleteComment_otherCompanyComment_returnsForbiddenAndKeepsComment() {
        ResponseEntity<String> response = send(HttpMethod.DELETE,
                "/api/documents/comments/" + other.comment().getId(), null, own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(commentCount(other.document())).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void documentComments_allowedCaller_canAddReadAndDelete(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        assertThat(send(HttpMethod.POST, commentsRoute(company.document()), commentBody(), access.token())
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(commentCount(company.document())).isEqualTo(2);

        assertThat(send(HttpMethod.GET, commentsRoute(company.document()), null, access.token()).getBody())
                .contains(company.comment().getCommentText());

        assertThat(send(HttpMethod.DELETE, "/api/documents/comments/" + company.comment().getId(), null,
                access.token()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(commentCount(company.document())).isEqualTo(1);
    }

    @Test
    void getCommentsByDocument_owningStudent_returnsOk() {
        ResponseEntity<String> response = send(HttpMethod.GET, commentsRoute(own.document()), null,
                tokenFor(own.applicant().getAppUser()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getCommentsByDocument_otherStudent_returnsForbidden() {
        ResponseEntity<String> response = send(HttpMethod.GET, commentsRoute(own.document()), null,
                tokenFor(other.applicant().getAppUser()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ==================== Document delete ====================

    @Test
    void deleteDocument_otherCompanyDocument_returnsForbiddenAndKeepsDocument() {
        ResponseEntity<String> response = send(HttpMethod.DELETE, deleteDocumentRoute(other.document()), null,
                own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(documentRepository.existsById(other.document().getId())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void deleteDocument_allowedCaller_deletesDocument(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();
        jdbc.update("DELETE FROM document_comments WHERE document_id = ?", company.document().getId());

        ResponseEntity<String> response = send(HttpMethod.DELETE, deleteDocumentRoute(company.document()), null,
                access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(documentRepository.existsById(company.document().getId())).isFalse();
    }

    @Test
    void deleteDocument_owningStudent_deletesDocument() {
        jdbc.update("DELETE FROM document_comments WHERE document_id = ?", own.document().getId());

        ResponseEntity<String> response = send(HttpMethod.DELETE, deleteDocumentRoute(own.document()), null,
                tokenFor(own.applicant().getAppUser()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(documentRepository.existsById(own.document().getId())).isFalse();
    }

    @Test
    void deleteDocument_otherStudent_returnsForbiddenAndKeepsDocument() {
        ResponseEntity<String> response = send(HttpMethod.DELETE, deleteDocumentRoute(own.document()), null,
                tokenFor(other.applicant().getAppUser()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(documentRepository.existsById(own.document().getId())).isTrue();
    }

    // ==================== Allocation history ====================

    @Test
    void getHistory_otherCompanyAllocation_returnsNotFoundAndHidesAdminEmail() {
        ResponseEntity<String> response = send(HttpMethod.GET, historyRoute(other.activeAllocation()), null,
                own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).doesNotContain(other.admin().getEmail());
    }

    @Test
    void getHistory_unknownAllocation_returnsNotFound() {
        ResponseEntity<String> response = send(HttpMethod.GET, "/api/room-allocation/999999999/history", null,
                own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void getHistory_allowedCaller_returnsHistory(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.GET, historyRoute(company.activeAllocation()), null,
                access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(company.admin().getEmail());
    }

    // ==================== Waiting list enqueue (GH-490) ====================

    @Test
    void enqueueWaitingList_otherCompanyApplication_returnsNotFoundAndQueuesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, ENQUEUE_ROUTE,
                enqueueBody(other.applicant(), other.application()), own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(waitingListCount(other.applicant())).isZero();
    }

    /** Otherwise a company could queue any student, who would then be offered its rooms. */
    @Test
    void enqueueWaitingList_applicationOfAnotherStudent_isRefusedAndQueuesNothing() {
        ResponseEntity<String> response = send(HttpMethod.POST, ENQUEUE_ROUTE,
                enqueueBody(other.applicant(), own.application()), own.adminToken());

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(waitingListCount(other.applicant())).isZero();
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void enqueueWaitingList_allowedCaller_queuesTheStudent(Caller caller) {
        Access access = allowed(caller);
        Company company = access.company();

        ResponseEntity<String> response = send(HttpMethod.POST, ENQUEUE_ROUTE,
                enqueueBody(company.applicant(), company.application()), access.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(waitingListCount(company.applicant())).isEqualTo(1);
    }

    // ==================== Room availability and eligible rooms (GH-490) ====================

    @ParameterizedTest
    @ValueSource(strings = {"availability", "is-available"})
    void roomAvailability_otherCompanyRoom_returnsNotFound(String check) {
        ResponseEntity<String> response = send(HttpMethod.GET,
                "/api/room-allocation/room/" + other.room().getId() + "/" + check, null, own.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void roomAvailability_allowedCaller_returnsAvailability(Caller caller) {
        Access access = allowed(caller);
        Long roomId = access.company().room().getId();

        for (String check : List.of("availability", "is-available")) {
            ResponseEntity<String> response = send(HttpMethod.GET,
                    "/api/room-allocation/room/" + roomId + "/" + check, null, access.token());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void eligibleRooms_otherCompanyBuilding_doesNotListItsRooms() {
        ResponseEntity<String> response = send(HttpMethod.GET,
                "/api/room-allocation/admin/student/" + own.applicant().getId() + "/eligible-rooms?buildingId="
                        + other.building().getId(),
                null, own.adminToken());

        assertThat(String.valueOf(response.getBody())).doesNotContain(other.room().getRoomNumber());
    }

    // ==================== Helpers ====================

    private static String enqueueBody(Student student, Application application) {
        return "{\"studentId\":" + student.getId() + ",\"applicationId\":" + application.getId() + "}";
    }

    private int waitingListCount(Student student) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM waiting_list_entry WHERE student_id = ?", Integer.class,
                student.getId());
    }

    private Access allowed(Caller caller) {
        return switch (caller) {
            case TENANT_ADMIN_ON_OWN_COMPANY -> new Access(own.adminToken(), own);
            case PLATFORM_ADMIN -> new Access(platformAdminToken, other);
            case PLATFORM_STAFF -> new Access(platformStaffToken, other);
        };
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private static String reviewBody(Long documentId) {
        return """
                {"documentUpdates":[{"documentId":%d,"newStatus":"APPROVED"}]}""".formatted(documentId);
    }

    private static String moveRoute(RoomAllocation allocation) {
        return "/api/admin/allocation/" + allocation.getId() + "/move";
    }

    private static String moveBody(Room newRoom) {
        return """
                {"newRoomId":%d,"reason":"Moving rooms"}""".formatted(newRoom.getId());
    }

    private static String offerBody(Student student, Room room, Application application) {
        return """
                {"studentId":%d,"roomId":%d,"applicationId":%s}""".formatted(
                student.getId(), room.getId(), application == null ? "null" : application.getId());
    }

    private static String allocateBody(Application application, Room room) {
        return """
                {"applicationId":%d,"roomId":%d}""".formatted(application.getId(), room.getId());
    }

    private static String attributesBody(Room room) {
        return """
                {"roomId":%d,"accessibilityCompliant":true,"minYearOfStudy":2,"maxYearOfStudy":4}"""
                .formatted(room.getId());
    }

    private static String commentsRoute(Document document) {
        return "/api/documents/" + document.getId() + "/comments";
    }

    private static String commentBody() {
        return """
                {"commentText":"A comment long enough to pass validation"}""";
    }

    private static String deleteDocumentRoute(Document document) {
        return "/api/documents/delete/" + document.getId();
    }

    private static String historyRoute(RoomAllocation allocation) {
        return "/api/room-allocation/" + allocation.getId() + "/history";
    }

    private String documentStatus(Document document) {
        return jdbc.queryForObject("SELECT document_status FROM document WHERE id = ?", String.class, document.getId());
    }

    private int commentCount(Document document) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM document_comments WHERE document_id = ?", Integer.class,
                document.getId());
    }

    private Long allocatedRoomId(RoomAllocation allocation) {
        return jdbc.queryForObject("SELECT room_id FROM room_allocation WHERE id = ?", Long.class, allocation.getId());
    }

    private int allocationCount(Student student) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM room_allocation WHERE student_id = ?", Integer.class,
                student.getId());
    }

    private int attributesCount(Room room) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM room_attributes WHERE room_id = ?", Integer.class,
                room.getId());
    }

    private Integer minYearOfStudy(Room room) {
        return jdbc.queryForObject("SELECT min_year_of_study FROM room_attributes WHERE room_id = ?", Integer.class,
                room.getId());
    }

    private String tokenFor(AppUser user) {
        return jwtUtil.generateToken(new AppUserDetails(
                user.getEmail(), "", user.getUserRole().name(), user.getTenantId()));
    }

    private static String ids(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    // ==================== Seeding ====================

    private Company seedCompany(String name) {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(name + " Company " + System.nanoTime())
                .isActive(true)
                .build());
        tenantIds.add(tenant.getId());

        Building building = new Building();
        building.setName(name + " Hall " + System.nanoTime());
        building.setAddress("1 Test Street");
        building.setTotalFloors(3);
        building.setTenantId(tenant.getId());
        building = buildingRepository.save(building);

        Room room = saveRoom(building);
        Room spareRoom = saveRoom(building);
        RoomAttributes attributes = roomAttributesRepository.save(RoomAttributes.builder()
                .room(room)
                .accessibilityCompliant(false)
                .minYearOfStudy(1)
                .maxYearOfStudy(1)
                .build());

        AppUser admin = saveUser(UserRole.TENANT_ADMIN, tenant.getId());

        Student applicant = saveStudent(UserRole.STUDENT);
        Application application = saveApplication(applicant, tenant.getId(), ApplicationStatus.APPROVED);
        Document document = saveDocument(applicant);
        DocumentComment comment = documentCommentRepository.save(DocumentComment.builder()
                .document(document)
                .admin(admin)
                .commentText("Seeded comment " + System.nanoTime())
                .build());

        Student resident = saveStudent(UserRole.STUDENT);
        RoomAllocation activeAllocation = saveAllocation(resident, room, tenant.getId(), AllocationStatus.ACTIVE);
        allocationAuditLogRepository.save(AllocationAuditLog.builder()
                .allocation(activeAllocation)
                .actionType(AllocationAuditActionType.OVERRIDE)
                .adminUser(admin)
                .reason("Seeded history")
                .build());

        return new Company(tenant, building, room, spareRoom, attributes, applicant, application, document, comment,
                resident, activeAllocation, admin, tokenFor(admin));
    }

    private RoomAllocation saveOffer(Company company) {
        TenantContext.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        RoomAllocation offer = saveAllocation(saveStudent(UserRole.STUDENT), company.room(),
                company.tenant().getId(), AllocationStatus.OFFERED);
        TenantContext.clear();
        return offer;
    }

    private Room saveRoom(Building building) {
        Room room = new Room();
        room.setRoomNumber("R" + System.nanoTime() % 1_000_000_000L);
        room.setBuilding(building);
        room.setTenantId(building.getTenantId());
        room.setCapacity(4);
        room.setCurrentOccupancy(0);
        room.setStatus("AVAILABLE");
        return roomRepository.save(room);
    }

    private AppUser saveUser(UserRole role, Long tenantId) {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "." + System.nanoTime() + "@test.com")
                .firstName("Test")
                .lastName("User")
                .userPassword("$2a$10$hashed")
                .userRole(role)
                .tenantId(tenantId)
                .emailVerified(true)
                .mustChangePassword(false)
                .build());
        userIds.add(user.getId());
        return user;
    }

    private Student saveStudent(UserRole role) {
        Student student = new Student();
        student.setAppUser(saveUser(role, TenantContext.PLATFORM_TENANT_ID));
        student.setStudentNumber("S-" + System.nanoTime());
        student.setPhoneNumber("0123456789");
        student.setGender("Female");
        student = studentRepository.save(student);
        studentIds.add(student.getId());
        return student;
    }

    private Application saveApplication(Student student, Long tenantId, ApplicationStatus status) {
        return applicationRepository.save(Application.builder()
                .academicYear(testCalendar().year())
                .intake(testCalendar().intake())
                .applicationCode("APP-" + System.nanoTime())
                .applicationDate(LocalDate.now())
                .status(status)
                .student(student)
                .tenantId(tenantId)
                .build());
    }

    private RoomAllocation saveAllocation(Student student, Room room, Long tenantId, AllocationStatus status) {
        RoomAllocation allocation = new RoomAllocation();
        allocation.setStudent(student);
        allocation.setRoom(room);
        allocation.setTenantId(tenantId);
        allocation.setStatus(status);
        allocation.setAllocationDate(LocalDateTime.now());
        return roomAllocationRepository.save(allocation);
    }

    private Document saveDocument(Student student) {
        DocumentType type = new DocumentType();
        type.setTenantId(TenantContext.PLATFORM_TENANT_ID);
        type.setTypeName("Cross company " + System.nanoTime());
        type.setIsRequired(false);
        type = documentTypeRepository.save(type);
        documentTypeIds.add(type.getId());

        return documentRepository.save(Document.builder()
                .tenantId(TenantContext.PLATFORM_TENANT_ID)
                .documentName("proof.pdf")
                .documentPath("Student/" + student.getId() + "/proof-" + System.nanoTime() + ".pdf")
                .documentStatus(DocumentStatus.PENDING)
                .student(student)
                .documentType(type)
                .build());
    }
}
