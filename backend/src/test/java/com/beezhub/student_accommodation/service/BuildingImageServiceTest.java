package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.model.dto.BuildingImageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingImage;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingImageServiceTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioConfig minioConfig;
    @Mock
    private BuildingImageRepository buildingImageRepository;
    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private BuildingImageService service;

    private Building building;
    private BuildingImage image1;
    private BuildingImage image2;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() throws Exception {
        building = new Building();
        building.setId(1L);

        image1 = new BuildingImage();
        image1.setId(101L);
        image1.setBuilding(building);
        image1.setObjectPath("path1");
        image1.setPrimary(true);
        image1.setDisplayOrder(0);

        image2 = new BuildingImage();
        image2.setId(102L);
        image2.setBuilding(building);
        image2.setObjectPath("path2");
        image2.setPrimary(false);
        image2.setDisplayOrder(1);

        validFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test data".getBytes());

        lenient().when(minioConfig.getBucketName()).thenReturn("test-bucket");
    }

    private void mockPresignedUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://presigned.url");
    }

    @Test
    void uploadImages_ValidFile_ReturnsResponses() throws Exception {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        mockPresignedUrl();

        List<BuildingImageResponse> responses = service.uploadImages(1L, List.of(validFile));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIsPrimary()).isTrue();
        verify(buildingImageRepository).save(any(BuildingImage.class));
    }

    @Test
    void uploadImages_EmptyFile_ThrowsException() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(emptyFile)));
    }

    @Test
    void uploadImages_LargeFile_ThrowsException() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(largeFile)));
    }

    @Test
    void uploadImages_InvalidType_ThrowsException() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(txtFile)));
    }

    @Test
    void uploadImages_MinioError_ThrowsException() throws Exception {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("Minio error"));

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(validFile)));
    }
    
    @Test
    void uploadImages_BuildingNotFound_ThrowsException() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(validFile)));
    }

    @Test
    void getImages_ReturnsResponses() throws Exception {
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(image1, image2));
        mockPresignedUrl();

        List<BuildingImageResponse> responses = service.getImages(1L);

        assertThat(responses).hasSize(2);
    }

    @Test
    void deleteImage_PrimaryImage_PromotesNextAndDeletes() throws Exception {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(image2));

        service.deleteImage(1L, 101L);

        verify(buildingImageRepository).delete(image1);
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        verify(buildingImageRepository).save(image2);
        assertThat(image2.getPrimary()).isTrue();
    }

    @Test
    void deleteImage_NonPrimary_JustDeletes() throws Exception {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));

        service.deleteImage(1L, 102L);

        verify(buildingImageRepository).delete(image2);
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        verify(buildingImageRepository, never()).save(any());
    }

    @Test
    void deleteImage_MinioError_SwallowsException() throws Exception {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));
        doThrow(new RuntimeException("Minio error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        service.deleteImage(1L, 102L);

        verify(buildingImageRepository).delete(image2);
    }

    @Test
    void deleteImage_WrongBuilding_ThrowsException() {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));

        assertThrows(RuntimeException.class, () -> service.deleteImage(2L, 101L));
    }
    
    @Test
    void deleteImage_NotFound_ThrowsException() {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteImage(1L, 101L));
    }

    @Test
    void setPrimaryImage_UnsetsOldAndSetsNew() throws Exception {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.of(image1));
        when(buildingImageRepository.save(image2)).thenReturn(image2);
        mockPresignedUrl();

        BuildingImageResponse response = service.setPrimaryImage(1L, 102L);

        verify(buildingImageRepository).saveAndFlush(image1);
        assertThat(image1.getPrimary()).isFalse();
        verify(buildingImageRepository).save(image2);
        assertThat(image2.getPrimary()).isTrue();
        assertThat(response.getIsPrimary()).isTrue();
    }
    
    @Test
    void setPrimaryImage_WrongBuilding_ThrowsException() {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));

        assertThrows(RuntimeException.class, () -> service.setPrimaryImage(2L, 102L));
    }
    
    @Test
    void setPrimaryImage_NotFound_ThrowsException() {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.setPrimaryImage(1L, 102L));
    }

    @Test
    void getPrimaryImageUrl_Exists_ReturnsUrl() throws Exception {
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.of(image1));
        mockPresignedUrl();

        String url = service.getPrimaryImageUrl(1L);

        assertThat(url).isEqualTo("http://presigned.url");
    }

    @Test
    void getPrimaryImageUrl_NotExists_ReturnsNull() {
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.empty());

        String url = service.getPrimaryImageUrl(1L);

        assertThat(url).isNull();
    }
    
    @Test
    void getPrimaryImageUrl_MinioError_ReturnsNull() throws Exception {
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.of(image1));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new RuntimeException("Minio Error"));

        String url = service.getPrimaryImageUrl(1L);

        assertThat(url).isNull();
    }

    // ---- Image cap tests ----

    @Test
    void uploadImages_WithinLimit_Succeeds() throws Exception {
        // 6 existing + 3 incoming = 9 (within limit)
        List<BuildingImage> existing = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> {
                    BuildingImage img = new BuildingImage();
                    img.setBuilding(building);
                    return img;
                }).toList();
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(existing);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        mockPresignedUrl();

        List<MultipartFile> files = List.of(validFile, validFile, validFile);
        List<BuildingImageResponse> responses = service.uploadImages(1L, files);

        assertThat(responses).hasSize(3);
    }

    @Test
    void uploadImages_ReachesExactlyTen_Succeeds() throws Exception {
        // 9 existing + 1 incoming = 10 (exactly at limit)
        List<BuildingImage> existing = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> {
                    BuildingImage img = new BuildingImage();
                    img.setBuilding(building);
                    return img;
                }).toList();
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(existing);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(201L);
            return saved;
        });
        mockPresignedUrl();

        List<BuildingImageResponse> responses = service.uploadImages(1L, List.of(validFile));

        assertThat(responses).hasSize(1);
    }

    @Test
    void uploadImages_ExceedsLimit_ThrowsException() {
        // 9 existing + 2 incoming = 11 (exceeds limit)
        List<BuildingImage> existing = java.util.stream.IntStream.range(0, 9)
                .mapToObj(i -> {
                    BuildingImage img = new BuildingImage();
                    img.setBuilding(building);
                    return img;
                }).toList();
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(existing);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.uploadImages(1L, List.of(validFile, validFile)));

        assertThat(ex.getMessage()).contains("cannot have more than 10 images");
        assertThat(ex.getMessage()).contains("current: 9");
        assertThat(ex.getMessage()).contains("requested: 2");
    }

    @Test
    void uploadImages_AlreadyAtLimit_ThrowsException() {
        // 10 existing + 1 incoming = 11 (already at limit)
        List<BuildingImage> existing = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> {
                    BuildingImage img = new BuildingImage();
                    img.setBuilding(building);
                    return img;
                }).toList();
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(existing);

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(validFile)));
    }
}