# Application Submission Flow Implementation

## Overview
This document describes the complete application submission flow implementation for the Student Accommodation System.

## Features Implemented

### 1. **Enhanced Security & Authorization**
- Students can only submit applications for themselves
- Students can only view and cancel their own applications
- Admin users have full access to all applications
- Proper authentication checks on all endpoints

### 2. **Custom Exceptions**
Created specific exceptions for better error handling:
- `ApplicationNotFoundException` - When an application is not found
- `StudentNotFoundException` - When a student is not found
- `DuplicateApplicationException` - When a student tries to submit multiple pending applications
- `UnauthorizedAccessException` - When a user tries to access resources they're not authorized for

### 3. **Enhanced DTOs**

#### ApplicationRequest
```java
{
  "studentId": 123
}
```

#### ApplicationStatusUpdateRequest
```java
{
  "status": "APPROVED|REJECTED|PENDING",
  "remarks": "Optional remarks"
}
```

#### ApplicationResponse (Enhanced)
```json
{
  "id": 1,
  "applicationCode": "APP-2025-001",
  "applicationDate": "2025-11-11",
  "status": "PENDING",
  "createdAt": "2025-11-11T10:30:00",
  "updatedAt": "2025-11-11T10:30:00",
  "studentId": 123,
  "studentNumber": "STU2025001",
  "studentName": "John Doe",
  "studentEmail": "john.doe@example.com"
}
```

### 4. **API Endpoints**

#### Student Endpoints

**Submit Application**
```
POST /api/applications/submit
Authorization: Bearer <token>
Role: STUDENT, ADMIN

Body: {
  "studentId": 123
}

Response: 201 CREATED
- Returns application details
- Prevents duplicate pending applications
- Auto-generates unique application code (APP-YYYY-XXX)
```

**Get My Applications**
```
GET /api/applications/my-applications
Authorization: Bearer <token>
Role: STUDENT

Response: 200 OK
- Returns all applications for the authenticated student
```

**View Specific Application**
```
GET /api/applications/{applicationId}
Authorization: Bearer <token>
Role: STUDENT, ADMIN

Response: 200 OK
- Students can only view their own applications
- Admins can view any application
```

**Cancel Application**
```
DELETE /api/applications/{applicationId}
Authorization: Bearer <token>
Role: STUDENT

Response: 204 NO CONTENT
- Only pending applications can be canceled
- Students can only cancel their own applications
```

#### Admin Endpoints

**Get All Applications**
```
GET /api/applications
Authorization: Bearer <token>
Role: ADMIN

Response: 200 OK
- Returns all applications in the system
```

**Get Applications by Status**
```
GET /api/applications/status/{status}
Authorization: Bearer <token>
Role: ADMIN

Parameters:
- status: PENDING | APPROVED | REJECTED

Response: 200 OK
- Returns all applications with the specified status
```

**Get Applications by Student ID**
```
GET /api/applications/student/{studentId}
Authorization: Bearer <token>
Role: ADMIN

Response: 200 OK
- Returns all applications for a specific student
```

**Update Application Status**
```
PATCH /api/applications/{applicationId}/status
Authorization: Bearer <token>
Role: ADMIN

Body: {
  "status": "APPROVED",
  "remarks": "Application approved"
}

Response: 200 OK
- Updates the status of an application
```

### 5. **Business Logic Enhancements**

#### Application Code Generation
- Format: `APP-{YEAR}-{SEQUENCE}`
- Example: `APP-2025-001`, `APP-2025-002`
- Automatically increments sequence per year
- Thread-safe generation

#### Duplicate Prevention
- Students cannot have multiple pending applications
- Checked before submission
- Returns 409 CONFLICT if duplicate exists

#### Status Management
- New applications start with `PENDING` status
- Only admins can change status
- Valid statuses: `PENDING`, `APPROVED`, `REJECTED`

#### Timestamps
- `createdAt` - Set automatically on creation
- `updatedAt` - Updated automatically on any modification
- Uses JPA lifecycle callbacks

### 6. **Error Handling**

All endpoints return appropriate HTTP status codes and error messages:

| Status Code | Description |
|------------|-------------|
| 200 OK | Request successful |
| 201 CREATED | Application created successfully |
| 204 NO CONTENT | Application deleted successfully |
| 400 BAD REQUEST | Invalid request data |
| 403 FORBIDDEN | Unauthorized access |
| 404 NOT FOUND | Resource not found |
| 409 CONFLICT | Duplicate application or business logic conflict |

Error Response Format:
```json
{
  "timestamp": "2025-11-11T10:30:00",
  "status": 409,
  "errors": ["Student already has a pending application"],
  "path": "/api/applications/submit"
}
```

### 7. **Security Features**

#### Role-Based Access Control
- `@PreAuthorize` annotations on all endpoints
- Role hierarchy respected (ADMIN > LANDLORD > STUDENT)
- JWT token validation on all protected endpoints

#### Data Access Control
- Students can only access their own data
- Admins have full system access
- Proper authorization checks in service layer

### 8. **Transaction Management**
- `@Transactional` on all write operations
- Ensures data consistency
- Automatic rollback on errors

## Database Schema

The `application` table structure:
```sql
CREATE TABLE application (
    id SERIAL PRIMARY KEY,
    application_code VARCHAR(50) NOT NULL UNIQUE,
    application_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    student_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);
```

## Testing Recommendations

### Unit Tests
- Test application submission with valid/invalid data
- Test duplicate application prevention
- Test authorization checks
- Test status updates
- Test application cancellation

### Integration Tests
- Test complete submission flow
- Test admin approval workflow
- Test concurrent submissions
- Test application code generation

### API Testing Examples

**Submit Application (Success)**
```bash
curl -X POST http://localhost:8080/api/applications/submit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1}'
```

**Submit Duplicate Application (Conflict)**
```bash
# Second submission should return 409 CONFLICT
curl -X POST http://localhost:8080/api/applications/submit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1}'
```

**Get My Applications**
```bash
curl -X GET http://localhost:8080/api/applications/my-applications \
  -H "Authorization: Bearer <token>"
```

**Admin: Update Status**
```bash
curl -X PATCH http://localhost:8080/api/applications/1/status \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "APPROVED", "remarks": "Application approved"}'
```

**Cancel Application**
```bash
curl -X DELETE http://localhost:8080/api/applications/1 \
  -H "Authorization: Bearer <token>"
```

## Future Enhancements

1. **Email Notifications**
   - Send email when application is submitted
   - Notify student of status changes

2. **Application History**
   - Track all status changes
   - Store who made changes and when

3. **Document Requirements**
   - Link required documents to applications
   - Validate document completion before submission

4. **Application Workflow**
   - Add intermediate statuses (UNDER_REVIEW, AWAITING_DOCUMENTS)
   - Multi-step approval process

5. **Reporting**
   - Application statistics
   - Status distribution reports
   - Time-to-approval metrics

6. **Batch Operations**
   - Bulk status updates
   - Export applications to CSV/PDF

## Swagger Documentation

Access API documentation at:
```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with:
- Request/response examples
- Parameter descriptions
- Error codes
- Authorization requirements

## Summary

The application submission flow is now complete with:
✅ Secure endpoints with proper authorization
✅ Duplicate application prevention
✅ Comprehensive error handling
✅ Enhanced DTOs with student information
✅ Admin controls for application management
✅ Transaction safety
✅ Audit timestamps
✅ RESTful API design
✅ Swagger documentation
