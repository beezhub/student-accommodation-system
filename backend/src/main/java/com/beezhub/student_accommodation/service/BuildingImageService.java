package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + buildingId));
        tenantOwnershipGuard.assertOwned(building, "Building", buildingId);

        List<BuildingImageResponse> responses = new ArrayList<>();
        List<BuildingImage> existingImages = buildingImageRepository
                .findByBuilding_IdOrderByDisplayOrderAsc(buildingId);
        boolean isFirstImage = existingImages.isEmpty();

        int existingCount = existingImages.size();
        int incomingCount = files.size();
        if (existingCount + incomingCount > MAX_IMAGES_PER_BUILDING) {
            throw new RuntimeException(String.format(
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
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        tenantOwnershipGuard.assertOwned(image.getBuilding(), "Building", buildingId);

        if (!image.getBuilding().getId().equals(buildingId)) {
            throw new RuntimeException("Image does not belong to building: " + buildingId);
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
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        tenantOwnershipGuard.assertOwned(image.getBuilding(), "Building", buildingId);

        if (!image.getBuilding().getId().equals(buildingId)) {
            throw new RuntimeException("Image does not belong to building: " + buildingId);
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
                .map(image -> getPresignedUrl(image.getObjectPath()))
                .orElse(null);
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
            throw new RuntimeException("Error uploading file: " + file.getOriginalFilename(), e);
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

    private String getPresignedUrl(String objectPath) {
        String bucketName = minioConfig.getBucketName();
        try {
            // A presigned URL is served straight by object storage, so the type it was stored with
            // is the type the browser sees. Overriding it clamps objects written before this
            // change, which carry whatever the uploader declared, without a re-upload (GH-219).
            //
            // inline, not attachment: these are gallery images meant to render in an <img>. The
            // clamp is what makes that safe — the type can only ever be one of four image types,
            // never text/html.
            Map<String, String> responseOverrides = Map.of(
                    "response-content-type", ImageContentTypes.clamp(storedContentType(objectPath)),
                    "response-content-disposition", "inline");

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .method(Http.Method.GET)
                            .extraQueryParams(responseOverrides)
                            .build());
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", objectPath, e);
            return null;
        }
    }

    /** The type object storage holds for the object, or null when it cannot be read. */
    private String storedContentType(String objectPath) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectPath)
                            .build()).contentType();
        } catch (Exception e) {
            log.warn("Could not read stored content type for {}: {}", objectPath, e.getMessage());
            return null;
        }
    }

    private BuildingImageResponse toResponse(BuildingImage image) {
        BuildingImageResponse response = new BuildingImageResponse();
        response.setId(image.getId());
        response.setBuildingId(image.getBuilding().getId());
        response.setFileName(image.getFileName());
        response.setImageUrl(getPresignedUrl(image.getObjectPath()));
        response.setIsPrimary(image.getPrimary());
        response.setDisplayOrder(image.getDisplayOrder());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }
}
