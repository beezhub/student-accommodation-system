package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.config.MinioConfig;
import com.beezhub.student_accommodation.model.dto.BuildingImageResponse;
import com.beezhub.student_accommodation.model.entity.Building;
import com.beezhub.student_accommodation.model.entity.BuildingImage;
import com.beezhub.student_accommodation.repository.BuildingImageRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
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

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
    static final int MAX_IMAGES_PER_BUILDING = 10;

    @Transactional
    public List<BuildingImageResponse> uploadImages(Long buildingId, List<MultipartFile> files) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Building not found with id: " + buildingId));

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
            validateImage(file);

            String objectPath = buildObjectPath(buildingId, file);
            uploadFileToMinio(objectPath, file);

            BuildingImage image = new BuildingImage();
            image.setBuilding(building);
            image.setTenant(building.getTenant());
            image.setFileName(file.getOriginalFilename());
            image.setObjectPath(objectPath);
            image.setContentType(file.getContentType());
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

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid file type. Only images are allowed (JPEG, PNG, GIF, WebP)");
        }
    }

    private String buildObjectPath(Long buildingId, MultipartFile file) {
        String uuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        return String.format("buildings/%d/%s_%s", buildingId, uuid, originalFilename);
    }

    private void uploadFileToMinio(String objectPath, MultipartFile file) {
        String bucketName = minioConfig.getBucketName();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
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
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .method(Method.GET)
                            .build());
        } catch (Exception e) {
            log.error("Error generating presigned URL for: {}", objectPath, e);
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
