## ADDED Requirements

### Requirement: Building image count is capped at 10 per building
The system SHALL prevent uploading images that would cause a building's total image count to exceed 10. If an upload request would exceed this limit, the entire request SHALL be rejected with a `400 Bad Request` error.

#### Scenario: Admin uploads images within the limit
- **WHEN** an admin uploads 3 images to a building that already has 6 images (total would be 9)
- **THEN** the system accepts all 3 images and returns `200 OK` with the uploaded image details

#### Scenario: Admin uploads images that would exceed the limit
- **WHEN** an admin uploads 2 images to a building that already has 9 images (total would be 11)
- **THEN** the system rejects the entire upload with `400 Bad Request` and a message indicating the maximum of 10 images has been reached

#### Scenario: Admin uploads to a building already at the limit
- **WHEN** an admin attempts to upload any image to a building that already has exactly 10 images
- **THEN** the system rejects the upload with `400 Bad Request` indicating the building is at the maximum image limit

### Requirement: Admin can delete a building image
The system SHALL allow an admin to delete a specific image from a building via `DELETE /api/building/{buildingId}/images/{imageId}`.

#### Scenario: Admin deletes a non-primary image
- **WHEN** an admin sends `DELETE /api/building/1/images/5` and image 5 is not the primary image
- **THEN** the system deletes the image from the database and from MinIO storage, returning `204 No Content`

#### Scenario: Admin deletes the primary image
- **WHEN** an admin deletes the primary image of a building that has other images
- **THEN** the system promotes the next image (by display order) as the new primary image

#### Scenario: Admin deletes the only image
- **WHEN** an admin deletes the only remaining image of a building
- **THEN** the image is deleted and the building has no primary image

### Requirement: Admin can set a primary image for a building
The system SHALL allow an admin to designate any existing building image as the primary image via `PUT /api/building/{buildingId}/images/{imageId}/primary`.

#### Scenario: Admin sets a new primary image
- **WHEN** an admin sends `PUT /api/building/1/images/3/primary`
- **THEN** the previous primary image is unmarked, image 3 is marked as primary, and the system returns the updated image response

### Requirement: Admin image upload UI reflects the 10-image limit
The Angular admin building management UI SHALL display the current image count and disable the upload button when a building has reached 10 images.

#### Scenario: Admin views image dialog for a building at the limit
- **WHEN** an admin opens the image management dialog for a building with 10 images
- **THEN** the upload control is disabled and a message states "Maximum of 10 images reached"

#### Scenario: Admin views image dialog for a building below the limit
- **WHEN** an admin opens the image management dialog for a building with fewer than 10 images
- **THEN** the upload control is enabled and the remaining slots count is visible (e.g., "6 of 10 images used")
