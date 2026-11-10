package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.ClockConfig;
import com.beezhub.student_accommodation.exceptions.AcademicYearNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.exceptions.InvalidAcademicCalendarException;
import com.beezhub.student_accommodation.exceptions.ResourceInUseException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.mapper.AcademicYearMapper;
import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.repository.IntakeRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import com.beezhub.student_accommodation.model.entity.Intake;
import com.beezhub.student_accommodation.model.enums.IntakeType;
import com.beezhub.student_accommodation.service.tenant.TenantContext;
import com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    private static final Long COMPANY = 42L;
    private static final Long PLATFORM = TenantContext.PLATFORM_TENANT_ID;
    private static final Long UNIVERSITY = 7L;
    /** 10 November 2026, mid-morning in Johannesburg. */
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private AcademicYearMapper academicYearMapper;
    @Mock private RoomAllocationRepository roomAllocationRepository;
    @Mock private IntakeRepository intakeRepository;
    @Mock private IntakeWindowPolicy intakeWindowPolicy;

    private final TenantOwnershipGuard tenantOwnershipGuard = new TenantOwnershipGuard();
    private AcademicYearService service;

    @BeforeEach
    void setUp() {
        service = serviceAt(TODAY.atTime(10, 0));
        TenantContext.setTenantId(COMPANY);
        lenient().when(academicYearMapper.toResponse(any())).thenAnswer(inv -> {
            AcademicYear y = inv.getArgument(0);
            return AcademicYearResponse.builder().id(y.getId()).name(y.getName())
                    .institutionId(y.getInstitutionId()).build();
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── Create and the overlap rule ─────────────────────────────────────────────────────────

    @Test
    void createAcademicYear_companyAdmin_stampsTheCompanyAndSaves() {
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.findOverlapping(eq(COMPANY), any(), any())).thenReturn(List.of());
        when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createAcademicYear(request);

        verify(academicYearRepository).save(argThatYear(y -> COMPANY.equals(y.getTenantId())));
    }

    /** GH-439: the name check is scoped to the company, so another company's name is free. */
    @Test
    void createAcademicYear_nameUsedByTheSameCompanyAndInstitution_isRefused() {
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.existsByTenantIdAndInstitutionIdAndName(COMPANY, null, "2027")).thenReturn(true);

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("All institutions");
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    void createAcademicYear_overlapsAYearForTheSameInstitution_isRefusedNamingThatYear() {
        stubInstitution();
        AcademicYearRequest request = request("2027b", UNIVERSITY, LocalDate.of(2027, 6, 1), LocalDate.of(2028, 5, 31));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.findOverlapping(eq(COMPANY), any(), any()))
                .thenReturn(List.of(year(5L, "2027", UNIVERSITY, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30))));

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("2027 (2027-02-01 to 2027-11-30)")
                .hasMessageContaining("University X");
    }

    @Test
    void createAcademicYear_twoAllInstitutionsYearsOverlap_isRefused() {
        AcademicYearRequest request = request("2027b", null, LocalDate.of(2027, 6, 1), LocalDate.of(2028, 5, 31));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.findOverlapping(eq(COMPANY), any(), any()))
                .thenReturn(List.of(year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30))));

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("overlap 2027");
    }

    @Test
    void createAcademicYear_specificInstitutionOverAnAllInstitutionsYear_isAllowed() {
        stubInstitution();
        AcademicYearRequest request = request("UX 2027", UNIVERSITY, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.findOverlapping(eq(COMPANY), any(), any()))
                .thenReturn(List.of(year(5L, "2027", null, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 12, 10))));
        when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.createAcademicYear(request)).doesNotThrowAnyException();
    }

    @Test
    void createAcademicYear_endsBeforeItStarts_isRefused() {
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 11, 30), LocalDate.of(2027, 2, 1));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    @Test
    void createAcademicYear_windowClosesBeforeItOpens_isRefused() {
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        AcademicYear entity = entityFrom(request);
        entity.setApplicationOpenDate(LocalDate.of(2027, 1, 1));
        entity.setApplicationCloseDate(LocalDate.of(2026, 12, 1));
        when(academicYearMapper.toEntity(request)).thenReturn(entity);

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(InvalidAcademicCalendarException.class);
    }

    @Test
    void createAcademicYear_unknownInstitution_isRefused() {
        AcademicYearRequest request = request("2027", 99L, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(institutionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAcademicYear(request))
                .isInstanceOf(InstitutionNotFoundException.class);
    }

    @Test
    void createAcademicYear_noTenantInContext_isAPlatformYear() {
        TenantContext.clear();
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createAcademicYear(request);

        verify(academicYearRepository).save(argThatYear(y -> PLATFORM.equals(y.getTenantId())));
    }

    // ── Update and delete ───────────────────────────────────────────────────────────────────

    @Test
    void updateAcademicYear_ownYear_excludesItselfFromTheChecks() {
        AcademicYear existing = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 12, 5));
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.findOverlapping(eq(COMPANY), any(), any())).thenReturn(List.of(existing));
        when(academicYearRepository.save(existing)).thenReturn(existing);

        service.updateAcademicYear(5L, request);

        verify(academicYearMapper).updateEntity(request, existing);
        verify(academicYearRepository).existsByTenantIdAndInstitutionIdAndNameAndIdNot(COMPANY, null, "2027", 5L);
    }

    @Test
    void createAcademicYear_noWindowGiven_fillsItInFromTheStartDate() {
        AcademicYearRequest request = request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        request.setApplicationOpenDate(null);
        request.setApplicationCloseDate(null);
        when(academicYearMapper.toEntity(request)).thenReturn(entityFrom(request));
        when(intakeWindowPolicy.windowFor(COMPANY, LocalDate.of(2027, 2, 1))).thenReturn(
                new IntakeWindowPolicy.Window(LocalDate.of(2026, 10, 4), LocalDate.of(2027, 1, 18)));
        when(academicYearRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createAcademicYear(request);

        verify(academicYearRepository).save(argThatYear(y -> LocalDate.of(2026, 10, 4).equals(y.getApplicationOpenDate())
                && LocalDate.of(2027, 1, 18).equals(y.getApplicationCloseDate())));
    }

    @Test
    void updateAcademicYear_datesMoved_returnsHowManyApplicationsAndAllocationsItHas() {
        AcademicYear existing = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(academicYearRepository.save(existing)).thenReturn(existing);
        when(applicationRepository.countByAcademicYear_Id(5L)).thenReturn(12L);
        when(roomAllocationRepository.countByAcademicYear_Id(5L)).thenReturn(4L);

        AcademicYearResponse response = service.updateAcademicYear(5L,
                request("2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 12, 5)));

        assertThat(response.getApplicationCount()).isEqualTo(12L);
        assertThat(response.getAllocationCount()).isEqualTo(4L);
    }

    @Test
    void updateAcademicYear_wouldLeaveAnIntakeOutside_isRefusedNamingIt() {
        AcademicYear existing = year(5L, "2027", null, LocalDate.of(2027, 8, 1), LocalDate.of(2027, 11, 30));
        Intake semester2 = Intake.builder().id(9L).name("Semester 2").intakeType(IntakeType.SEMESTER)
                .stayStartDate(LocalDate.of(2027, 7, 15)).stayEndDate(LocalDate.of(2027, 11, 30)).build();
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(intakeRepository.findByAcademicYear_IdOrderByStayStartDateAscNameAsc(5L)).thenReturn(List.of(semester2));

        assertThatThrownBy(() -> service.updateAcademicYear(5L,
                request("2027", null, LocalDate.of(2027, 8, 1), LocalDate.of(2027, 11, 30))))
                .isInstanceOf(InvalidAcademicCalendarException.class)
                .hasMessageContaining("Semester 2");
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    void getAllAcademicYears_listsEachYearsIntakesFromOneQuery() {
        PageRequest page = PageRequest.of(0, 20);
        AcademicYear y = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        Intake annual = Intake.builder().id(9L).academicYear(y).name("Annual").intakeType(IntakeType.ANNUAL)
                .stayStartDate(y.getStartDate()).stayEndDate(y.getEndDate())
                .applicationOpenDate(TODAY.minusDays(1)).applicationCloseDate(TODAY.plusDays(1)).build();
        when(academicYearRepository.findByTenantId(COMPANY, page)).thenReturn(new PageImpl<>(List.of(y), page, 1));
        when(intakeRepository.findByAcademicYear_IdInOrderByStayStartDateAscNameAsc(List.of(5L)))
                .thenReturn(List.of(annual));

        PageResponse<AcademicYearResponse> result = service.getAllAcademicYears(page);

        assertThat(result.getContent().get(0).getIntakes()).singleElement()
                .satisfies(i -> {
                    assertThat(i.getName()).isEqualTo("Annual");
                    assertThat(i.getAcademicYearId()).isEqualTo(5L);
                    assertThat(i.getIsApplicationWindowOpen()).isTrue();
                });
    }

    @Test
    void updateAcademicYear_anotherCompanysYear_isNotFound() {
        AcademicYear other = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        other.setTenantId(99L);
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateAcademicYear(5L, request("x", null, TODAY, TODAY.plusDays(1))))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    void updateAcademicYear_missing_isNotFound() {
        when(academicYearRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAcademicYear(5L, request("x", null, TODAY, TODAY.plusDays(1))))
                .isInstanceOf(AcademicYearNotFoundException.class);
    }

    @Test
    void deleteAcademicYear_withApplications_isRefused() {
        AcademicYear existing = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(applicationRepository.existsByAcademicYear_Id(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAcademicYear(5L)).isInstanceOf(ResourceInUseException.class);
        verify(academicYearRepository, never()).delete(any());
    }

    @Test
    void deleteAcademicYear_withoutApplications_deletes() {
        AcademicYear existing = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.deleteAcademicYear(5L);

        verify(academicYearRepository).delete(existing);
    }

    // ── Reads ───────────────────────────────────────────────────────────────────────────────

    /** GH-439: a company admin sees their own company's years. */
    @Test
    void getAllAcademicYears_companyAdmin_readsOnlyTheCompanysYears() {
        PageRequest page = PageRequest.of(0, 20);
        when(academicYearRepository.findByTenantId(COMPANY, page)).thenReturn(new PageImpl<>(
                List.of(year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30))), page, 1));

        PageResponse<AcademicYearResponse> result = service.getAllAcademicYears(page);

        assertThat(result.getContent()).extracting(AcademicYearResponse::getName).containsExactly("2027");
        verify(academicYearRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    void getAllAcademicYears_platformAdmin_readsEveryCompany() {
        TenantContext.setTenantId(PLATFORM);
        PageRequest page = PageRequest.of(0, 20);
        when(academicYearRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(), page, 0));

        service.getAllAcademicYears(page);

        verify(academicYearRepository, never()).findByTenantId(anyLong(), any());
    }

    @Test
    void getAcademicYearById_platformYear_isReadable() {
        AcademicYear platformYear = year(5L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        platformYear.setTenantId(PLATFORM);
        when(academicYearRepository.findById(5L)).thenReturn(Optional.of(platformYear));

        assertThat(service.getAcademicYearById(5L).getName()).isEqualTo("2027");
    }

    // ── Current year by date ────────────────────────────────────────────────────────────────

    @Test
    void findCurrentAcademicYear_todayInsideAYear_returnsThatYear() {
        AcademicYear y2026 = year(1L, "2026", null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        AcademicYear y2027 = year(2L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearRepository.findNotEndedByTenant(COMPANY, TODAY)).thenReturn(List.of(y2026, y2027));

        assertThat(service.findCurrentAcademicYear(COMPANY, null)).contains(y2026);
    }

    @Test
    void findCurrentAcademicYear_betweenTwoYears_returnsTheNextOne() {
        service = serviceAt(LocalDate.of(2026, 12, 15).atTime(9, 0));
        AcademicYear y2027 = year(2L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        when(academicYearRepository.findNotEndedByTenant(COMPANY, LocalDate.of(2026, 12, 15)))
                .thenReturn(List.of(y2027));

        assertThat(service.findCurrentAcademicYear(COMPANY, null)).contains(y2027);
    }

    @Test
    void findCurrentAcademicYear_noYearsAtAll_isEmpty() {
        when(academicYearRepository.findNotEndedByTenant(anyLong(), eq(TODAY))).thenReturn(List.of());

        assertThat(service.findCurrentAcademicYear(COMPANY, null)).isEmpty();
    }

    @Test
    void findCurrentAcademicYear_specificInstitutionOverlapsAllInstitutions_specificWins() {
        AcademicYear all = year(1L, "2026", null, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 12, 10));
        AcademicYear specific = year(2L, "UX 2026", UNIVERSITY, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        when(academicYearRepository.findNotEndedByTenant(COMPANY, TODAY)).thenReturn(List.of(all, specific));

        assertThat(service.findCurrentAcademicYear(COMPANY, UNIVERSITY)).contains(specific);
        assertThat(service.findCurrentAcademicYear(COMPANY, 8L)).contains(all);
        assertThat(service.findCurrentAcademicYear(COMPANY, null)).contains(all);
    }

    @Test
    void findCurrentAcademicYear_companyHasNoYears_fallsBackToThePlatformYears() {
        AcademicYear platformYear = year(1L, "2026", null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        when(academicYearRepository.findNotEndedByTenant(COMPANY, TODAY)).thenReturn(List.of());
        when(academicYearRepository.findNotEndedByTenant(PLATFORM, TODAY)).thenReturn(List.of(platformYear));

        assertThat(service.findCurrentAcademicYear(COMPANY, null)).contains(platformYear);
    }

    @Test
    void getCurrentAcademicYear_none_isNotFound() {
        when(academicYearRepository.findNotEndedByTenant(anyLong(), eq(TODAY))).thenReturn(List.of());

        assertThatThrownBy(() -> service.getCurrentAcademicYear(null))
                .isInstanceOf(AcademicYearNotFoundException.class);
    }

    @Test
    void getCurrentAcademicYear_companyAdmin_returnsTheCompanysYear() {
        AcademicYear own = year(1L, "2026", null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        when(academicYearRepository.findNotEndedByTenant(COMPANY, TODAY)).thenReturn(List.of(own));

        assertThat(service.getCurrentAcademicYear(null).getName()).isEqualTo("2026");
    }

    // ── Time zone ───────────────────────────────────────────────────────────────────────────

    /** GH-494: "closes 30 June" means midnight in South Africa, not 02:00 the next day. */
    @Test
    void isApplicationWindowOpen_halfPastMidnightOnFirstJulyInJohannesburg_windowClosing30JuneIsClosed() {
        service = serviceAt(LocalDate.of(2027, 7, 1).atTime(0, 30));
        AcademicYear year = year(1L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        year.setApplicationOpenDate(LocalDate.of(2027, 3, 1));
        year.setApplicationCloseDate(LocalDate.of(2027, 6, 30));

        assertThat(service.isApplicationWindowOpen(year)).isFalse();
    }

    @Test
    void isApplicationWindowOpen_lateOnTheClosingDay_isOpen() {
        service = serviceAt(LocalDate.of(2027, 6, 30).atTime(23, 30));
        AcademicYear year = year(1L, "2027", null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 11, 30));
        year.setApplicationOpenDate(LocalDate.of(2027, 3, 1));
        year.setApplicationCloseDate(LocalDate.of(2027, 6, 30));

        assertThat(service.isApplicationWindowOpen(year)).isTrue();
        assertThat(service.isApplicationWindowOpen(null)).isFalse();
    }

    @Test
    void toResponse_setsTheWindowFlagFromTheClock() {
        AcademicYear year = year(1L, "2026", null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 11, 30));
        year.setApplicationOpenDate(TODAY.minusDays(1));
        year.setApplicationCloseDate(TODAY.plusDays(1));

        assertThat(service.toResponse(year).getIsApplicationWindowOpen()).isTrue();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────

    private AcademicYearService serviceAt(LocalDateTime johannesburgTime) {
        Clock clock = Clock.fixed(ZonedDateTime.of(johannesburgTime, ClockConfig.CALENDAR_ZONE).toInstant(),
                ClockConfig.CALENDAR_ZONE);
        return new AcademicYearService(academicYearRepository, applicationRepository, roomAllocationRepository,
                intakeRepository, intakeWindowPolicy, institutionRepository, academicYearMapper,
                tenantOwnershipGuard, clock);
    }

    private void stubInstitution() {
        Institution institution = new Institution();
        institution.setId(UNIVERSITY);
        institution.setName("University X");
        when(institutionRepository.findById(UNIVERSITY)).thenReturn(Optional.of(institution));
    }

    private static AcademicYearRequest request(String name, Long institutionId, LocalDate start, LocalDate end) {
        return AcademicYearRequest.builder().name(name).institutionId(institutionId).startDate(start).endDate(end)
                .applicationOpenDate(start.minusMonths(4)).applicationCloseDate(start.minusDays(14)).build();
    }

    private static AcademicYear entityFrom(AcademicYearRequest request) {
        return AcademicYear.builder().name(request.getName()).institutionId(request.getInstitutionId())
                .startDate(request.getStartDate()).endDate(request.getEndDate())
                .applicationOpenDate(request.getApplicationOpenDate())
                .applicationCloseDate(request.getApplicationCloseDate()).build();
    }

    private static AcademicYear year(Long id, String name, Long institutionId, LocalDate start, LocalDate end) {
        return AcademicYear.builder().id(id).tenantId(COMPANY).name(name).institutionId(institutionId)
                .startDate(start).endDate(end).build();
    }

    private static AcademicYear argThatYear(java.util.function.Predicate<AcademicYear> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
