package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.RoomAttributesRequest;
import com.beezhub.student_accommodation.model.dto.RoomAttributesResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAttributes;
import com.beezhub.student_accommodation.model.enums.GenderRestriction;
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
class RoomAttributesServiceTest {

    @Mock private RoomAttributesRepository roomAttributesRepository;
    @Mock private RoomRepository roomRepository;

    @InjectMocks
    private RoomAttributesService service;

    private Building building;
    private Room room;
    private RoomAttributes attributes;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Block B");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("102");
        room.setBuilding(building);

        attributes = RoomAttributes.builder()
                .id(200L)
                .room(room)
                .accessibilityCompliant(true)
                .genderRestriction(GenderRestriction.ANY)
                .minYearOfStudy(1)
                .maxYearOfStudy(4)
                .build();
    }

    // ===== getAttributesByRoomId =====

    @Test
    void getAttributesByRoomId_attributesExist_returnsMapped() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(attributes));

        RoomAttributesResponse response = service.getAttributesByRoomId(10L);

        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getRoomNumber()).isEqualTo("102");
        assertThat(response.getBuildingId()).isEqualTo(1L);
        assertThat(response.getBuildingName()).isEqualTo("Block B");
        assertThat(response.getAccessibilityCompliant()).isTrue();
    }

    @Test
    void getAttributesByRoomId_noAttributes_returnsDefault() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.empty());

        RoomAttributesResponse response = service.getAttributesByRoomId(10L);

        assertThat(response.getRoomId()).isEqualTo(10L);
        assertThat(response.getAccessibilityCompliant()).isFalse();
        assertThat(response.getGenderRestriction()).isEqualTo(GenderRestriction.ANY);
    }

    @Test
    void getAttributesByRoomId_roomNotFound_throwsException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAttributesByRoomId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ===== getAttributesByBuildingId =====

    @Test
    void getAttributesByBuildingId_returnsListForBuilding() {
        when(roomAttributesRepository.findByRoom_Building_Id(1L)).thenReturn(List.of(attributes));

        List<RoomAttributesResponse> responses = service.getAttributesByBuildingId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBuildingId()).isEqualTo(1L);
    }

    @Test
    void getAttributesByBuildingId_noAttributes_returnsEmpty() {
        when(roomAttributesRepository.findByRoom_Building_Id(1L)).thenReturn(List.of());

        List<RoomAttributesResponse> responses = service.getAttributesByBuildingId(1L);

        assertThat(responses).isEmpty();
    }

    // ===== saveAttributes =====

    @Test
    void saveAttributes_newAttributes_createsAndReturns() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setRoomId(10L);
        request.setAccessibilityCompliant(true);
        request.setGenderRestriction(GenderRestriction.FEMALE_ONLY);
        request.setMinYearOfStudy(1);
        request.setMaxYearOfStudy(3);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.empty());
        when(roomAttributesRepository.save(any(RoomAttributes.class))).thenAnswer(inv -> {
            RoomAttributes saved = inv.getArgument(0);
            saved.setRoom(room);
            return saved;
        });

        RoomAttributesResponse response = service.saveAttributes(request, "admin@test.com");

        assertThat(response.getRoomId()).isEqualTo(10L);
        verify(roomAttributesRepository).save(any(RoomAttributes.class));
    }

    @Test
    void saveAttributes_existingAttributes_updatesFields() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setRoomId(10L);
        request.setGenderRestriction(GenderRestriction.MALE_ONLY);
        request.setMinYearOfStudy(2);
        request.setMaxYearOfStudy(4);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(attributes));
        when(roomAttributesRepository.save(attributes)).thenReturn(attributes);

        service.saveAttributes(request, "admin@test.com");

        assertThat(attributes.getGenderRestriction()).isEqualTo(GenderRestriction.MALE_ONLY);
        assertThat(attributes.getMinYearOfStudy()).isEqualTo(2);
        assertThat(attributes.getMaxYearOfStudy()).isEqualTo(4);
    }

    @Test
    void saveAttributes_invalidYearRange_throwsException() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setRoomId(10L);
        request.setMinYearOfStudy(4);
        request.setMaxYearOfStudy(2); // min > max — invalid

        assertThatThrownBy(() -> service.saveAttributes(request, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum year");
    }

    @Test
    void saveAttributes_roomNotFound_throwsException() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setRoomId(99L);

        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveAttributes(request, "admin@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveAttributes_nullAccessibilityCompliant_doesNotUpdateField() {
        RoomAttributesRequest request = new RoomAttributesRequest();
        request.setRoomId(10L);
        request.setAccessibilityCompliant(null); // null — should not overwrite
        attributes.setAccessibilityCompliant(true);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(attributes));
        when(roomAttributesRepository.save(attributes)).thenReturn(attributes);

        service.saveAttributes(request, "admin");

        assertThat(attributes.getAccessibilityCompliant()).isTrue(); // unchanged
    }

    // ===== bulkSaveAttributes =====

    @Test
    void bulkSaveAttributes_multipleRequests_savesAll() {
        RoomAttributesRequest req1 = new RoomAttributesRequest();
        req1.setRoomId(10L);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(attributes));
        when(roomAttributesRepository.save(any(RoomAttributes.class))).thenReturn(attributes);

        List<RoomAttributesResponse> responses = service.bulkSaveAttributes(List.of(req1, req1), "admin");

        assertThat(responses).hasSize(2);
        verify(roomAttributesRepository, times(2)).save(any(RoomAttributes.class));
    }

    // ===== deleteAttributes =====

    @Test
    void deleteAttributes_existing_deletesSuccessfully() {
        when(roomAttributesRepository.findByRoom_Id(10L)).thenReturn(Optional.of(attributes));

        service.deleteAttributes(10L, "admin");

        verify(roomAttributesRepository).delete(attributes);
    }

    @Test
    void deleteAttributes_notFound_throwsException() {
        when(roomAttributesRepository.findByRoom_Id(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAttributes(99L, "admin"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
