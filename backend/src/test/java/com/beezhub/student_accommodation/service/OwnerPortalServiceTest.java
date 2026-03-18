package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.model.dto.OwnerBuildingSummaryDto;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingOwner;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.repository.BuildingOwnerRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerPortalService tests")
class OwnerPortalServiceTest {

    @Mock private BuildingOwnerRepository buildingOwnerRepository;
    @Mock private BuildingRepository buildingRepository;

    @InjectMocks
    private OwnerPortalService service;

    private BuildingOwner ownerProfile(Long id) {
        BuildingOwner profile = new BuildingOwner();
        profile.setId(id);
        return profile;
    }

    private Room room(String status) {
        Room r = new Room();
        r.setStatus(status);
        return r;
    }

    private Building buildingWithRooms(Long id, String name, String address, List<Room> rooms) {
        Building b = new Building();
        b.setId(id);
        b.setName(name);
        b.setAddress(address);
        b.setRooms(rooms);
        return b;
    }

    @Test
    @DisplayName("returns building summaries filtered by owner profile ID")
    void getOwnerBuildings_ownerExists_returnsSummaries() {
        BuildingOwner profile = ownerProfile(5L);
        when(buildingOwnerRepository.findById(5L)).thenReturn(Optional.of(profile));

        Building b1 = buildingWithRooms(1L, "Block A", "123 Main St",
                List.of(room("OCCUPIED"), room("AVAILABLE"), room("OCCUPIED")));
        Building b2 = buildingWithRooms(2L, "Block B", "456 Side St",
                List.of(room("AVAILABLE")));
        when(buildingRepository.findByOwner_Id(5L)).thenReturn(List.of(b1, b2));

        List<OwnerBuildingSummaryDto> result = service.getOwnerBuildings(5L);

        assertThat(result).hasSize(2);

        OwnerBuildingSummaryDto summary1 = result.get(0);
        assertThat(summary1.getName()).isEqualTo("Block A");
        assertThat(summary1.getTotalRooms()).isEqualTo(3);
        assertThat(summary1.getOccupiedRooms()).isEqualTo(2L);

        OwnerBuildingSummaryDto summary2 = result.get(1);
        assertThat(summary2.getName()).isEqualTo("Block B");
        assertThat(summary2.getTotalRooms()).isEqualTo(1);
        assertThat(summary2.getOccupiedRooms()).isEqualTo(0L);

        verify(buildingRepository).findByOwner_Id(5L);
    }

    @Test
    @DisplayName("returns empty list when owner has no buildings")
    void getOwnerBuildings_noBuildings_returnsEmptyList() {
        BuildingOwner profile = ownerProfile(7L);
        when(buildingOwnerRepository.findById(7L)).thenReturn(Optional.of(profile));
        when(buildingRepository.findByOwner_Id(7L)).thenReturn(List.of());

        List<OwnerBuildingSummaryDto> result = service.getOwnerBuildings(7L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when owner profile not found")
    void getOwnerBuildings_ownerNotFound_throwsResourceNotFound() {
        when(buildingOwnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnerBuildings(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Owner profile not found");
    }

    @Test
    @DisplayName("counts FULL rooms as occupied")
    void getOwnerBuildings_fullRoomsCountAsOccupied() {
        BuildingOwner profile = ownerProfile(3L);
        when(buildingOwnerRepository.findById(3L)).thenReturn(Optional.of(profile));

        Building b = buildingWithRooms(1L, "Block C", "789 Park Ave",
                List.of(room("FULL"), room("OCCUPIED"), room("AVAILABLE")));
        when(buildingRepository.findByOwner_Id(3L)).thenReturn(List.of(b));

        List<OwnerBuildingSummaryDto> result = service.getOwnerBuildings(3L);

        assertThat(result.get(0).getOccupiedRooms()).isEqualTo(2L);
    }

    @Test
    @DisplayName("uses formattedAddress when available")
    void getOwnerBuildings_usesFormattedAddressWhenSet() {
        BuildingOwner profile = ownerProfile(4L);
        when(buildingOwnerRepository.findById(4L)).thenReturn(Optional.of(profile));

        Building b = buildingWithRooms(1L, "Block D", "raw address", List.of());
        b.setFormattedAddress("123 Formatted St, City");
        when(buildingRepository.findByOwner_Id(4L)).thenReturn(List.of(b));

        List<OwnerBuildingSummaryDto> result = service.getOwnerBuildings(4L);

        assertThat(result.get(0).getAddress()).isEqualTo("123 Formatted St, City");
    }
}
