## Why

Students currently have no way to explore available accommodation buildings before creating an account or submitting an application, making it difficult to make informed choices. Admins also lack a mechanism to showcase buildings with photos, reducing the appeal of the housing portal as an advertising platform.

## What Changes

- New public building browsing page accessible without authentication
- Building image gallery: admins can upload up to 10 photos per building
- Application form updated to allow students to select up to 3 preferred buildings from the browsable catalogue
- Building detail view showing images, description, room types, and amenities
- Admin building management extended with image upload/delete capabilities

## Capabilities

### New Capabilities

- `public-building-browse`: Unauthenticated browsing of buildings — list view and detail view with image gallery (max 10 images per building)
- `building-image-management`: Admin capability to upload, reorder, and delete building images stored in MinIO (max 10 per building)
- `application-building-preference`: Student application preference updated to allow selecting up to 3 preferred buildings from the public catalogue

### Modified Capabilities

- `frontend`: Public routing added for building browse pages (no auth guard); application form updated with multi-building preference selector

## Impact

- **Backend**: `Building` entity and `BuildingImage` entity already exist; new endpoints needed for public access and image management APIs
- **Frontend**: New public route `/buildings` and `/buildings/:id`; updated application form component; admin building management component updated for image upload UI
- **Storage**: MinIO bucket used for building images (already configured)
- **Security**: Building browse endpoints must be publicly accessible (no JWT required); image upload endpoints remain admin-only
- **Database**: No schema changes needed — `building_image` table already exists
