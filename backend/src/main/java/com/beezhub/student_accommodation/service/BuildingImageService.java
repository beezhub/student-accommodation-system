package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.ResourceNotFoundException;
import com.beezhub.student_accommodation.exceptions.FileStorageException;
import com.beezhub.student_accommodation.exceptions.BuildingNotFoundException;
import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.model.dto.BuildingImageContentResponse;
import com.beezhub.student_accommodation.model.dto.BuildingImageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingImage;
import com.beezhub.student_accommodation.exceptions.FileValidationException;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.service.document.ValidationResult;
import com.beezhub.student_accommodation.service.image.ImageContentTypes;
import com.beezhub.student_accommodation.service.image.ImageUploadValidator;
import io.minio.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingImageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final BuildingImageRepository buildingImageRepository;
    private final BuildingRepository buildingRepository;
    private final com.beezhub.student_accommodation.service.tenant.TenantOwnershipGuard tenantOwnershipGuard;
    private final ImageUploadValidator imageUploadValidator;

    static final int MAX_IMAGES_PER_BUILDING = 10;

    @Transactional
    public List<BuildingImageResponse> uploadImages(Long buildingId, List<MultipartFile> files) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new BuildingNotFoundException("Building not found with id: " + buildingId));
        tenantOwnershipGuard.assertOwned(building, "Building", buildingId);

        List<BuildingImageResponse> responses = new ArrayList<>();
        List<BuildingImage> existingImages = buildingImageRepository
                .findByBuilding_IdOrderByDisplayOrderAsc(buildingId);
        boolean isFirstImage = existingImages.isEmpty();

        int existingCount = existingImages.size();
        int incomingCount = files.size();
        if (existingCount + incomingCount > MAX_IMAGES_PER_BUILDING) {
            // FileValidationException, so the refusal is 422 rather than the 400 a bare
            // RuntimeException produced: the upload itself is what cannot be accepted.
            throw new FileValidationException(String.format(
                    "Building cannot have more than %d images (current: %d, requested: %d)",
                    MAX_IMAGES_PER_BUILDING, existingCount, incomingCount));
        }

        for (MultipartFile file : files) {
            // The type comes from the file's own bytes. It used to come from the multipart part
            // header, checked only against a list of strings the uploader also controlled, then
            // stored on the row and sent to object storage as the object's type (GH-219).
            String contentType = validateImage(file);

            String objectPath = buildObjectPath(buildingId, file);
            uploadFileToMinio(objectPath, file, contentType);

            BuildingImage image = new BuildingImage();
            image.setBuilding(building);
            // tenant_id is the writable side of the dual mapping now; the tenant association is
            // read-only. Set it from the building rather than relying on TenantListener, so a
            // platform admin uploading for a tenant still stamps that tenant.
            image.setTenantId(building.getTenantId());
            image.setFileName(file.getOriginalFilename());
            image.setObjectPath(objectPath);
            image.setContentType(contentType);
            image.setFileSize(file.getSize());
            image.setPrimary(isFirstImage && responses.isEmpty());
            image.setDisplayOrder(existingImages.size() + responses.size());

            BuildingImage savedImage = buildingImageRepository.save(image);
            responses.add(toResponse(savedImage));
            log.info("Uploaded image {} for building {}", savedImage.getId(), buildingId);
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<BuildingImageResponse> getImages(Long buildingId) {
        List<BuildingImage> images = buildingImageRepository.findByBuilding_IdOrderByDisplayOrderAsc(buildingId);
        return images.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(Long buildingId, Long imageId) {
        BuildingImage image = buildingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        tenantOwnershipGuard.assertOwned(image.getBuilding(), "Building", buildingId);

        if (!image.getBuilding().getId().equals(buildingId)) {
            // 404 rather than a mismatch error: from the caller's side that image does not exist
            // under that building, and saying more would confirm it exists under another.
            throw new ResourceNotFoundException(
                    "Image " + imageId + " not found for building " + buildingId);
        }

        boolean wasPrimary = image.getPrimary();
        String objectPath = image.getObjectPath();

        // Delete from database first
        buildingImageRepository.delete(image);

        // Delete from Minio
        deleteFileFromMinio(objectPath);

        // If deleted image was primary, promote the next image
        if (wasPrimary) {
            List<BuildingImage> remainingImages = buildingImageRepository
                    .findByBuilding_IdOrderByDisplayOrderAsc(buildingId);
            if (!remainingImages.isEmpty()) {
                BuildingImage newPrimary = remainingImages.get(0);
                newPrimary.setPrimary(true);
                buildingImageRepository.save(newPrimary);
                log.info("Promoted image {} to primary for building {}", newPrimary.getId(), buildingId);
            }
        }

        log.info("Deleted image {} from building {}", imageId, buildingId);
    }

    @Transactional
    public BuildingImageResponse setPrimaryImage(Long buildingId, Long imageId) {
        BuildingImage image = buildingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        tenantOwnershipGuard.assertOwned(image.getBuilding(), "Building", buildingId);

        if (!image.getBuilding().getId().equals(buildingId)) {
            // 404 rather than a mismatch error: from the caller's side that image does not exist
            // under that building, and saying more would confirm it exists under another.
            throw new ResourceNotFoundException(
                    "Image " + imageId + " not found for building " + buildingId);
        }

        // Unset current primary - must flush before setting new primary
        // to avoid unique constraint violation on idx_building_image_primary
        buildingImageRepository.findByBuilding_IdAndPrimaryTrue(buildingId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setPrimary(false);
                    buildingImageRepository.saveAndFlush(currentPrimary);
                });

        // Set new primary
        image.setPrimary(true);
        BuildingImage savedImage = buildingImageRepository.save(image);
        log.info("Set image {} as primary for building {}", imageId, buildingId);

        return toResponse(savedImage);
    }

    @Transactional(readOnly = true)
    public String getPrimaryImageUrl(Long buildingId) {
        return buildingImageRepository.findByBuilding_IdAndPrimaryTrue(buildingId)
                .map(image -> buildContentPath(buildingId, image.getId()))
                .orElse(null);
    }

    /**
     * Streams a building image's bytes through the backend rather than handing the browser a
     * presigned MinIO URL. A presigned URL is signed against {@code MINIO_ENDPOINT}, which is the
     * compose-internal hostname in every deployed environment, and MinIO's API port is not
     * published on top of that, so no presigned URL for this bucket can ever resolve from a
     * browser outside a developer's own machine (GH-268).
     */
    @Transactional(readOnly = true)
    public BuildingImageContentResponse getImageContent(Long buildingId, Long imageId) {
        BuildingImage image = buildingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getBuilding().getId().equals(buildingId)) {
            // 404 rather than a mismatch error, matching deleteImage/setPrimaryImage: from the
            // caller's side that image does not exist under that building.
            throw new ResourceNotFoundException(
                    "Image " + imageId + " not found for building " + buildingId);
        }

        // The stored type, not a fresh read of object storage: uploadImages already ran the bytes
        // through ImageUploadValidator and wrote the resolved type to this column (GH-219). The
        // clamp still runs here so a row written before that fix cannot be served as anything but
        // one of the four allowed image types.
        String contentType = ImageContentTypes.clamp(image.getContentType());
        InputStream stream = downloadFileFromMinio(image.getObjectPath());
        return BuildingImageContentResponse.builder()
                .resource(new InputStreamResource(stream))
                .contentType(contentType)
                .build();
    }

    /** Returns the content type read from the file's bytes, or throws if it is not a valid image. */
    private String validateImage(MultipartFile file) {
        ValidationResult result = imageUploadValidator.validate(file);
        if (!result.clean()) {
            throw new FileValidationException("Image rejected: " + result.reason());
        }
        return result.resolvedContentType();
    }

    private String buildObjectPath(Long buildingId, MultipartFile file) {
        String uuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        return String.format("buildings/%d/%s_%s", buildingId, uuid, originalFilename);
    }

    private void uploadFileToMinio(String objectPath, MultipartFile file, String contentType) {
        String bucketName = minioConfig.getBucketName();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(is, file.getSize(), -1L)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error uploading file: " + file.getOriginalFilename(), e);
        }
    }

    private void deleteFileFromMinio(String objectPath) {
        String bucketName = minioConfig.getBucketName();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build());
        } catch (Exception e) {
            log.error("Error deleting file from Minio: {}", objectPath, e);
            // Don't throw exception - database deletion already succeeded
        }
    }

    private InputStream downloadFileFromMinio(String objectPath) {
        String bucketName = minioConfig.getBucketName();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .build());
        } catch (Exception e) {
            throw new FileStorageException("Error downloading image: " + objectPath, e);
        }
    }

    /**
     * The path the frontend should request the image from, relative to the API root (no
     * {@code /api} prefix — {@code environment.apiUrl} in the frontend already supplies that, the
     * same way it does for every other building endpoint).
     */
    private String buildContentPath(Long buildingId, Long imageId) {
        return String.format("/building/%d/images/%d/content", buildingId, imageId);
    }

    private BuildingImageResponse toResponse(BuildingImage image) {
        BuildingImageResponse response = new BuildingImageResponse();
        response.setId(image.getId());
        response.setBuildingId(image.getBuilding().getId());
        response.setFileName(image.getFileName());
        response.setImageUrl(buildContentPath(image.getBuilding().getId(), image.getId()));
        response.setIsPrimary(image.getPrimary());
        response.setDisplayOrder(image.getDisplayOrder());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }
}
