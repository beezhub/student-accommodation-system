package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.BuildingImageContentResponse;
import com.beezhub.student_accommodation.model.dto.BuildingImageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingImage;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.service.document.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingImageServiceTest {

    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private BuildingImageRepository buildingImageRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;

    /**
     * The real validator, not a mock: what this fix is about is whether the bytes are looked at
     * at all, and a stubbed validator would assert nothing about that (GH-219).
     */
    @org.mockito.Spy
    private com.beezhub.student_accommodation.service.image.ImageUploadValidator imageUploadValidator =
            new com.beezhub.student_accommodation.service.image.ImageUploadValidator(5 * 1024 * 1024);

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
        image1.setContentType("image/jpeg");
        image1.setPrimary(true);
        image1.setDisplayOrder(0);

        image2 = new BuildingImage();
        image2.setId(102L);
        image2.setBuilding(building);
        image2.setObjectPath("path2");
        image2.setContentType("image/png");
        image2.setPrimary(false);
        image2.setDisplayOrder(1);

        // Real JPEG magic bytes. This used to be the string "test data" and the upload was
        // accepted, because nothing looked past the multipart part header (GH-219).
        validFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes());

    }

    /** A minimal file whose leading bytes are a real JPEG signature. */
    private static byte[] jpegBytes() {
        byte[] bytes = new byte[64];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    /** A minimal file whose leading bytes are a real PNG signature. */
    private static byte[] pngBytes() {
        byte[] bytes = new byte[64];
        byte[] magic = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        System.arraycopy(magic, 0, bytes, 0, magic.length);
        return bytes;
    }

    @Test
    void uploadImages_ValidFile_ReturnsResponses() throws Exception {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        List<BuildingImageResponse> responses = service.uploadImages(1L, List.of(validFile));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getIsPrimary()).isTrue();
        // A path relative to the API root, not a presigned storage URL the browser can never reach
        // in a deployed environment (GH-268).
        assertThat(responses.get(0).getImageUrl()).isEqualTo("/building/1/images/100/content");
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
        byte[] large = new byte[6 * 1024 * 1024];
        System.arraycopy(jpegBytes(), 0, large, 0, 3);
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", large);

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(largeFile)));
    }

    @Test
    void uploadImages_InvalidType_ThrowsException() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(txtFile)));
    }

    // ── GH-219: the declared content type is no longer trusted ─────────────────────────────

    @Test
    void uploadImages_declaredTypeIsIgnored_theBytesDecide() throws Exception {
        // Declares image/png; the bytes are a JPEG. The stored type must be what the file is.
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile mislabelled =
                new MockMultipartFile("file", "test.jpg", "image/png", jpegBytes());

        service.uploadImages(1L, List.of(mislabelled));

        ArgumentCaptor<BuildingImage> saved = ArgumentCaptor.forClass(BuildingImage.class);
        verify(buildingImageRepository).save(saved.capture());
        assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");

        verify(fileStorageService).upload(anyString(), any(InputStream.class), anyLong(), eq("image/jpeg"));
    }

    @Test
    void uploadImages_htmlDeclaredAsAnImage_isRejected() {
        // The old check compared the part header against an allow-list the uploader controlled, so
        // this file was stored and served as image/png.
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile html = new MockMultipartFile(
                "file", "evil.png", "image/png", "<html><script>alert(1)</script></html>".getBytes());

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(html)));
        verify(buildingImageRepository, never()).save(any());
    }

    @Test
    void uploadImages_extensionAndBytesDisagree_isRejected() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        MockMultipartFile mismatched =
                new MockMultipartFile("file", "photo.png", "image/png", jpegBytes());

        assertThrows(RuntimeException.class, () -> service.uploadImages(1L, List.of(mismatched)));
        verify(buildingImageRepository, never()).save(any());
    }

    @Test
    void uploadImages_pngIsAccepted() throws Exception {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile png = new MockMultipartFile("file", "test.png", "image/png", pngBytes());

        assertThat(service.uploadImages(1L, List.of(png))).hasSize(1);
    }

    // ── Streaming image content (GH-268) ────────────────────────────────────────────────────

    @Test
    void getImageContent_Exists_ReturnsResourceAndContentType() throws Exception {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));
        when(fileStorageService.download("path1")).thenReturn(new ByteArrayInputStream("bytes".getBytes()));

        BuildingImageContentResponse content = service.getImageContent(1L, 101L);

        assertThat(content.getContentType()).isEqualTo("image/jpeg");
        assertThat(content.getResource()).isNotNull();
        verify(fileStorageService).download("path1");
    }

    @Test
    void getImageContent_clampsALegacyOrMissingContentType() throws Exception {
        // Rows written before GH-219 may carry no content type, or one it never validated.
        image1.setContentType(null);
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));
        when(fileStorageService.download("path1")).thenReturn(new ByteArrayInputStream("bytes".getBytes()));

        BuildingImageContentResponse content = service.getImageContent(1L, 101L);

        assertThat(content.getContentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void getImageContent_WrongBuilding_ThrowsException() {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));

        assertThrows(RuntimeException.class, () -> service.getImageContent(2L, 101L));
    }

    @Test
    void getImageContent_NotFound_ThrowsException() {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getImageContent(1L, 101L));
    }

    @Test
    void uploadImages_StorageError_ThrowsException() throws Exception {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
        when(fileStorageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Storage error"));

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

        List<BuildingImageResponse> responses = service.getImages(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getImageUrl()).isEqualTo("/building/1/images/101/content");
        assertThat(responses.get(1).getImageUrl()).isEqualTo("/building/1/images/102/content");
    }

    @Test
    void deleteImage_PrimaryImage_PromotesNextAndDeletes() throws Exception {
        when(buildingImageRepository.findById(101L)).thenReturn(Optional.of(image1));
        when(buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(image2));

        service.deleteImage(1L, 101L);

        verify(buildingImageRepository).delete(image1);
        verify(fileStorageService).delete("path1");
        verify(buildingImageRepository).save(image2);
        assertThat(image2.getPrimary()).isTrue();
    }

    @Test
    void deleteImage_NonPrimary_JustDeletes() throws Exception {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));

        service.deleteImage(1L, 102L);

        verify(buildingImageRepository).delete(image2);
        verify(fileStorageService).delete("path2");
        verify(buildingImageRepository, never()).save(any());
    }

    @Test
    void deleteImage_StorageError_SwallowsException() throws Exception {
        when(buildingImageRepository.findById(102L)).thenReturn(Optional.of(image2));
        doThrow(new RuntimeException("Storage error")).when(fileStorageService).delete("path2");

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
    void getPrimaryImageUrl_Exists_ReturnsUrl() {
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.of(image1));

        String url = service.getPrimaryImageUrl(1L);

        assertThat(url).isEqualTo("/building/1/images/101/content");
    }

    @Test
    void getPrimaryImageUrl_NotExists_ReturnsNull() {
        when(buildingImageRepository.findByBuilding_IdAndPrimaryTrue(1L)).thenReturn(Optional.empty());

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
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(200L);
            return saved;
        });

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
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(i -> {
            BuildingImage saved = i.getArgument(0);
            saved.setId(201L);
            return saved;
        });

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