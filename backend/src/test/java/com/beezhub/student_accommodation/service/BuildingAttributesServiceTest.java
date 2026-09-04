package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.exceptions.RoomNotFoundException;
import com.beezhub.student_accommodation.model.dto.BuildingAttributesRequest;
import com.beezhub.student_accommodation.model.dto.BuildingAttributesResponse;
import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingAttributes;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
import com.beezhub.student_accommodation.repository.BuildingAttributesRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.RoomAttributesRepository;
import com.beezhub.student_accommodation.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingAttributesServiceTest {

    @Mock private BuildingAttributesRepository buildingAttributesRepository;
    @Mock private RoomAttributesRepository roomAttributesRepository;
    @Mock private BuildingRepository buildingRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks
    private BuildingAttributesService service;

    private Building building;
    private Room room;
    private BuildingAttributes buildingAttributes;
    private RoomAttributes roomAttributes;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Block A");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setBuilding(building);

        buildingAttributes = BuildingAttributes.builder()
                .id(100L)
                .building(building)
                .genderRestriction(GenderRestriction.ANY)
                .allowMixedGender(false)
                .build();

        roomAttributes = RoomAttributes.builder()
                .id(200L)
                .room(room)
                .accessibilityCompliant(false)
                .genderRestriction(GenderRestriction.ANY)
                .build();
    }

    // ===== getBuildingAttributes =====

    @Test
    void getBuildingAttributes_attributesExist_returnsMappedResponse() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttributes));

        BuildingAttributesResponse response = service.getBuildingAttributes(1L);

        assertThat(response.getBuildingId()).isEqualTo(1L);
        assertThat(response.getBuildingName()).isEqualTo("Block A");
    }

    @Test
    void getBuildingAttributes_noAttributes_returnsDefaultResponse() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());

        BuildingAttributesResponse response = service.getBuildingAttributes(1L);

        assertThat(response.getBuildingId()).isEqualTo(1L);
        assertThat(response.getGenderRestriction()).isEqualTo(GenderRestriction.ANY);
        assertThat(response.getAllowMixedGender()).isFalse();
    }

    @Test
    void getBuildingAttributes_buildingNotFound_throwsException() {
        when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBuildingAttributes(99L))
                .isInstanceOf(BuildingNotFoundException.class);
    }

    // ===== updateBuildingAttributes =====

    @Test
    void updateBuildingAttributes_existingAttributes_updatesAndSaves() {
        BuildingAttributesRequest request = new BuildingAttributesRequest();
        request.setGenderRestriction(GenderRestriction.FEMALE_ONLY);
        request.setAccessibilityFeatures("Ramp access");
        request.setDesirabilityScore(8);
        request.setAllowMixedGender(false);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttributes));
        when(buildingAttributesRepository.save(any(BuildingAttributes.class))).thenReturn(buildingAttributes);

        BuildingAttributesResponse response = service.updateBuildingAttributes(1L, request);

        assertThat(response).isNotNull();
        assertThat(buildingAttributes.getGenderRestriction()).isEqualTo(GenderRestriction.FEMALE_ONLY);
        verify(buildingAttributesRepository).save(buildingAttributes);
    }

    @Test
    void updateBuildingAttributes_noExistingAttributes_createsNew() {
        BuildingAttributesRequest request = new BuildingAttributesRequest();
        request.setGenderRestriction(GenderRestriction.MALE_ONLY);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.empty());
        when(buildingAttributesRepository.save(any(BuildingAttributes.class))).thenAnswer(inv -> inv.getArgument(0));

        BuildingAttributesResponse response = service.updateBuildingAttributes(1L, request);

        assertThat(response).isNotNull();
        verify(buildingAttributesRepository).save(any(BuildingAttributes.class));
    }

    @Test
    void updateBuildingAttributes_buildingNotFound_throwsException() {
        when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBuildingAttributes(99L, new BuildingAttributesRequest()))
                .isInstanceOf(BuildingNotFoundException.class);
    }

    @Test
    void updateBuildingAttributes_nullFields_doesNotOverwrite() {
        BuildingAttributesRequest request = new BuildingAttributesRequest();
        // All fields null — should not overwrite existing values
        buildingAttributes.setGenderRestriction(GenderRestriction.FEMALE_ONLY);
        buildingAttributes.setDesirabilityScore(9);
        buildingAttributes.setAllowMixedGender(true);

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingAttributesRepository.findByBuilding_Id(1L)).thenReturn(Optional.of(buildingAttributes));
        when(buildingAttributesRepository.save(any(BuildingAttributes.class))).thenReturn(buildingAttributes);

        service.updateBuildingAttributes(1L, request);

        assertThat(buildingAttributes.getGenderRestriction()).isEqualTo(GenderRestriction.FEMALE_ONLY);
        assertThat(buildingAttributes.getDesirabilityScore()).isEqualTo(9);
        assertThat(buildingAttributes.getAllowMixedGender()).isTrue();
    }

    // ===== getRoomAttributes =====

    @Test
    void getRoomAttributes_attributesExist_returnsMappedResponse() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(roomAttributes));

        RoomAttributesResponse response = service.getRoomAttributes(10L);

        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getRoomNumber()).isEqualTo("101");
    }

    @Test
    void getRoomAttributes_noAttributes_returnsDefaultResponse() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.empty());

        RoomAttributesResponse response = service.getRoomAttributes(10L);

        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getRoomNumber()).isEqualTo("101");
        assertThat(response.getBuildingId()).isEqualTo(1L);
        assertThat(response.getAccessibilityCompliant()).isFalse();
    }

    @Test
    void getRoomAttributes_roomNotFound_throwsException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRoomAttributes(99L))
                .isInstanceOf(RoomNotFoundException.class);
    }

    // ===== updateRoomAttributes =====

    @Test
    void updateRoomAttributes_existingAttributes_updatesAndSaves() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setAccessibilityCompliant(true);
        request.setGenderRestriction(GenderRestriction.FEMALE_ONLY);
        request.setMinYearOfStudy(1);
        request.setMaxYearOfStudy(3);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(roomAttributes));
        when(roomAttributesRepository.save(any(RoomAttributes.class))).thenReturn(roomAttributes);

        RoomAttributesResponse response = service.updateRoomAttributes(10L, request);

        assertThat(response).isNotNull();
        assertThat(roomAttributes.getAccessibilityCompliant()).isTrue();
        assertThat(roomAttributes.getGenderRestriction()).isEqualTo(GenderRestriction.FEMALE_ONLY);
        verify(roomAttributesRepository).save(roomAttributes);
    }

    @Test
    void updateRoomAttributes_noExistingAttributes_createsNew() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setAccessibilityCompliant(true);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.empty());
        when(roomAttributesRepository.save(any(RoomAttributes.class))).thenAnswer(inv -> {
            RoomAttributes saved = inv.getArgument(0);
            saved.setRoom(room);
            return saved;
        });

        RoomAttributesResponse response = service.updateRoomAttributes(10L, request);

        assertThat(response).isNotNull();
        verify(roomAttributesRepository).save(any(RoomAttributes.class));
    }

    @Test
    void updateRoomAttributes_roomNotFound_throwsException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoomAttributes(99L, new RoomAttributesRequest()))
                .isInstanceOf(RoomNotFoundException.class);
    }

    // ===== getRoomAttributesByBuilding =====

    @Test
    void getRoomAttributesByBuilding_buildingExists_returnsAttributes() {
        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(roomAttributesRepository.findByRoom_Building_Id(1L)).thenReturn(List.of(roomAttributes));

        List<RoomAttributesResponse> responses = service.getRoomAttributesByBuilding(1L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void getRoomAttributesByBuilding_buildingNotFound_throwsException() {
        when(buildingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getRoomAttributesByBuilding(99L))
                .isInstanceOf(BuildingNotFoundException.class);
    }

    @Test
    void getRoomAttributesByBuilding_noRoomAttributes_returnsEmptyList() {
        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(roomAttributesRepository.findByRoom_Building_Id(1L)).thenReturn(List.of());

        List<RoomAttributesResponse> responses = service.getRoomAttributesByBuilding(1L);

        assertThat(responses).isEmpty();
    }
}
