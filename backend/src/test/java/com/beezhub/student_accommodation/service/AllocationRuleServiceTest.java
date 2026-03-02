package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.AllocationRuleRequest;
import com.beezhub.student_accommodation.model.dto.AllocationRuleResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AllocationRule;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.repository.AllocationRuleRepository;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationRuleServiceTest {

    @Mock
    private AllocationRuleRepository allocationRuleRepository;
    
    @Mock
    private BuildingRepository buildingRepository;
    
    @Mock
    private AppUserRepository appUserRepository;
    
    @Mock
    private AuthService authService;

    @InjectMocks
    private AllocationRuleService allocationRuleService;

    private AppUser adminUser;
    private Building building;
    private AllocationRule rule;
    private AllocationRuleRequest validRequest;

    @BeforeEach
    void setUp() {
        adminUser = AppUser.builder()
                .id(1L)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .build();

        building = new Building();
        building.setId(1L);
        building.setName("Test Building");

        rule = AllocationRule.builder()
                .id(1L)
                .building(building)
                .ruleName("Test Rule")
                .preferenceMatchWeight(new BigDecimal("30"))
                .desirabilityWeight(new BigDecimal("30"))
                .accessibilityWeight(new BigDecimal("40"))
                .active(true)
                .priority(1)
                .createdBy(adminUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = AllocationRuleRequest.builder()
                .buildingId(1L)
                .ruleName("Test Rule")
                .preferenceMatchWeight(new BigDecimal("30"))
                .desirabilityWeight(new BigDecimal("30"))
                .accessibilityWeight(new BigDecimal("40"))
                .active(true)
                .priority(1)
                .build();
    }

    @Test
    void getRulesByBuilding_NullBuilding_ReturnsGlobalRules() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AllocationRule> page = new PageImpl<>(List.of(rule));
        when(allocationRuleRepository.findByBuildingIsNull(pageable)).thenReturn(page);

        PageResponse<AllocationRuleResponse> response = allocationRuleService.getRulesByBuilding(null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(allocationRuleRepository).findByBuildingIsNull(pageable);
        verify(buildingRepository, never()).existsById(any());
    }

    @Test
    void getRulesByBuilding_ValidBuildingId_ReturnsBuildingRules() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AllocationRule> page = new PageImpl<>(List.of(rule));
        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(allocationRuleRepository.findByBuilding_Id(1L, pageable)).thenReturn(page);

        PageResponse<AllocationRuleResponse> response = allocationRuleService.getRulesByBuilding(1L, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(allocationRuleRepository).findByBuilding_Id(1L, pageable);
    }

    @Test
    void getRulesByBuilding_InvalidBuildingId_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(buildingRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> 
                allocationRuleService.getRulesByBuilding(1L, pageable));
    }

    @Test
    void getRuleById_ExistingRule_ReturnsResponse() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));

        AllocationRuleResponse response = allocationRuleService.getRuleById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRuleName()).isEqualTo("Test Rule");
    }

    @Test
    void getRuleById_NonExistingRule_ThrowsException() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                allocationRuleService.getRuleById(1L));
    }

    @Test
    void createRule_ValidRequest_CreatesRule() {
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Test Rule")).thenReturn(false);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenReturn(rule);

        AllocationRuleResponse response = allocationRuleService.createRule(validRequest, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getRuleName()).isEqualTo("Test Rule");
        verify(allocationRuleRepository).save(any(AllocationRule.class));
    }

    @Test
    void createRule_InvalidWeights_ThrowsException() {
        validRequest.setPreferenceMatchWeight(new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> 
                allocationRuleService.createRule(validRequest, "admin@test.com"));
        
        verify(allocationRuleRepository, never()).save(any());
    }

    @Test
    void createRule_DuplicateName_ThrowsException() {
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Test Rule")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> 
                allocationRuleService.createRule(validRequest, "admin@test.com"));
    }
    
    @Test
    void createRule_DuplicateGlobalName_ThrowsException() {
        validRequest.setBuildingId(null);
        when(allocationRuleRepository.existsByBuildingIsNullAndRuleName("Test Rule")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> 
                allocationRuleService.createRule(validRequest, "admin@test.com"));
    }

    @Test
    void createRule_BuildingNotFound_ThrowsException() {
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Test Rule")).thenReturn(false);
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                allocationRuleService.createRule(validRequest, "admin@test.com"));
    }

    @Test
    void createRule_UserNotFound_ThrowsException() {
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Test Rule")).thenReturn(false);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
                allocationRuleService.createRule(validRequest, "admin@test.com"));
    }

    @Test
    void updateRule_ValidRequest_UpdatesRule() {
        AllocationRuleRequest updateReq = AllocationRuleRequest.builder()
                .buildingId(1L)
                .ruleName("Updated Rule")
                .preferenceMatchWeight(new BigDecimal("20"))
                .desirabilityWeight(new BigDecimal("40"))
                .accessibilityWeight(new BigDecimal("40"))
                .build();

        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Updated Rule")).thenReturn(false);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenAnswer(i -> i.getArgument(0));

        AllocationRuleResponse response = allocationRuleService.updateRule(1L, updateReq, "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getRuleName()).isEqualTo("Updated Rule");
        assertThat(response.getPreferenceMatchWeight()).isEqualTo(new BigDecimal("20"));
        verify(allocationRuleRepository).save(any(AllocationRule.class));
    }
    
    @Test
    void updateRule_SameName_SkipsDuplicateCheck() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenAnswer(i -> i.getArgument(0));

        AllocationRuleResponse response = allocationRuleService.updateRule(1L, validRequest, "admin@test.com");

        assertThat(response).isNotNull();
        verify(allocationRuleRepository, never()).existsByBuilding_IdAndRuleName(anyLong(), anyString());
    }

    @Test
    void updateRule_NullBuilding_RemovesBuilding() {
        AllocationRuleRequest updateReq = AllocationRuleRequest.builder()
                .buildingId(null)
                .ruleName("Test Rule")
                .preferenceMatchWeight(new BigDecimal("30"))
                .desirabilityWeight(new BigDecimal("30"))
                .accessibilityWeight(new BigDecimal("40"))
                .build();

        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenAnswer(i -> i.getArgument(0));

        AllocationRuleResponse response = allocationRuleService.updateRule(1L, updateReq, "admin@test.com");

        assertThat(response.getBuildingId()).isNull();
    }
    
    @Test
    void updateRule_UpdateDuplicateName_ThrowsException() {
        AllocationRuleRequest updateReq = AllocationRuleRequest.builder()
                .buildingId(1L)
                .ruleName("Duplicate Rule")
                .preferenceMatchWeight(new BigDecimal("30"))
                .desirabilityWeight(new BigDecimal("30"))
                .accessibilityWeight(new BigDecimal("40"))
                .build();

        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Duplicate Rule")).thenReturn(true);
        when(allocationRuleRepository.findByBuilding_IdAndRuleName(1L, "Duplicate Rule")).thenReturn(Optional.empty());

        assertThrows(DuplicateResourceException.class, () -> 
                allocationRuleService.updateRule(1L, updateReq, "admin@test.com"));
    }
    
    @Test
    void updateRule_UpdateDuplicateNameSameRule_UpdatesSuccessfully() {
        AllocationRuleRequest updateReq = AllocationRuleRequest.builder()
                .buildingId(1L)
                .ruleName("Updated Rule")
                .preferenceMatchWeight(new BigDecimal("30"))
                .desirabilityWeight(new BigDecimal("30"))
                .accessibilityWeight(new BigDecimal("40"))
                .build();

        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(allocationRuleRepository.existsByBuilding_IdAndRuleName(1L, "Updated Rule")).thenReturn(true);
        // Returns the same rule -> duplicate name check passes
        when(allocationRuleRepository.findByBuilding_IdAndRuleName(1L, "Updated Rule")).thenReturn(Optional.of(rule));
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenAnswer(i -> i.getArgument(0));

        AllocationRuleResponse response = allocationRuleService.updateRule(1L, updateReq, "admin@test.com");

        assertThat(response).isNotNull();
    }
    
    @Test
    void deleteRule_ExistingRule_SetsActiveToFalse() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        
        allocationRuleService.deleteRule(1L, "admin@test.com");
        
        assertThat(rule.getActive()).isFalse();
        verify(allocationRuleRepository).save(rule);
    }
    
    @Test
    void deleteRule_NonExistingRule_ThrowsException() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
                allocationRuleService.deleteRule(1L, "admin@test.com"));
    }
    
    @Test
    void activateRule_ExistingRule_SetsActiveToTrue() {
        rule.setActive(false);
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.of(rule));
        when(allocationRuleRepository.save(any(AllocationRule.class))).thenAnswer(i -> i.getArgument(0));
        
        AllocationRuleResponse response = allocationRuleService.activateRule(1L, "admin@test.com");
        
        assertThat(response.getActive()).isTrue();
        assertThat(rule.getActive()).isTrue();
        verify(allocationRuleRepository).save(rule);
    }
    
    @Test
    void activateRule_NonExistingRule_ThrowsException() {
        when(allocationRuleRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> 
                allocationRuleService.activateRule(1L, "admin@test.com"));
    }
}
