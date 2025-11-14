# Application Submission Flow - Quick Reference

## Endpoints Summary

### Student Endpoints
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/applications/submit` | STUDENT, ADMIN | Submit a new application |
| GET | `/api/applications/my-applications` | STUDENT | Get my applications |
| GET | `/api/applications/{id}` | STUDENT, ADMIN | Get application by ID |
| DELETE | `/api/applications/{id}` | STUDENT | Cancel pending application |

### Admin Endpoints
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/applications` | ADMIN | Get all applications |
| GET | `/api/applications/status/{status}` | ADMIN | Get applications by status |
| GET | `/api/applications/student/{studentId}` | ADMIN | Get applications by student |
| PATCH | `/api/applications/{id}/status` | ADMIN | Update application status |

## Key Features

### ✅ Security
- JWT authentication required
- Role-based access control
- Students can only access their own data
- Admins have full access

### ✅ Business Rules
- No duplicate pending applications
- Auto-generated unique application codes (APP-YYYY-XXX)
- Only pending applications can be canceled
- Status tracking with timestamps

### ✅ Error Handling
- Custom exceptions for different scenarios
- Proper HTTP status codes
- Detailed error messages
- Validation on all inputs

### ✅ Data Integrity
- Transactional operations
- Automatic timestamps (createdAt, updatedAt)
- Cascade delete protection
- Foreign key constraints

## Application Statuses
- `PENDING` - Initial status, awaiting review
- `APPROVED` - Application accepted
- `REJECTED` - Application declined

## Common Use Cases

### Student Flow
1. **Submit Application**
   - POST `/api/applications/submit` with `{"studentId": 123}`
   - Receives unique application code
   - Status set to PENDING

2. **Check Status**
   - GET `/api/applications/my-applications`
   - View all personal applications

3. **Cancel if Needed**
   - DELETE `/api/applications/{id}`
   - Only if status is PENDING

### Admin Flow
1. **View All Applications**
   - GET `/api/applications`
   - Or filter by status: GET `/api/applications/status/PENDING`

2. **Review Application**
   - GET `/api/applications/{id}`
   - View full details including student info

3. **Approve/Reject**
   - PATCH `/api/applications/{id}/status`
   - Set status to APPROVED or REJECTED

## Files Modified/Created

### New Files
- `ApplicationNotFoundException.java` - Custom exception
- `StudentNotFoundException.java` - Custom exception
- `DuplicateApplicationException.java` - Custom exception
- `UnauthorizedAccessException.java` - Custom exception
- `ApplicationRequest.java` - Submit request DTO
- `ApplicationStatusUpdateRequest.java` - Status update DTO
- `APPLICATION_SUBMISSION_FLOW.md` - Full documentation

### Modified Files
- `ApplicationController.java` - Enhanced with new endpoints
- `ApplicationService.java` - Complete business logic
- `ApplicationRepository.java` - New query methods
- `ApplicationMapper.java` - Enhanced mapping with student details
- `ApplicationResponse.java` - Added student info and timestamps
- `Application.java` - Added timestamp lifecycle callbacks
- `RestResponseExceptionHandler.java` - New exception handlers

## Testing the Flow

### 1. Submit Application (as Student)
```bash
POST /api/applications/submit
Authorization: Bearer <student-token>
Content-Type: application/json

{
  "studentId": 1
}

Expected: 201 CREATED
```

### 2. Try Duplicate (as Student)
```bash
POST /api/applications/submit
Authorization: Bearer <student-token>
Content-Type: application/json

{
  "studentId": 1
}

Expected: 409 CONFLICT
Error: "Student already has a pending application"
```

### 3. View My Applications (as Student)
```bash
GET /api/applications/my-applications
Authorization: Bearer <student-token>

Expected: 200 OK with array of applications
```

### 4. Approve Application (as Admin)
```bash
PATCH /api/applications/1/status
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "status": "APPROVED",
  "remarks": "All documents verified"
}

Expected: 200 OK with updated application
```

## Configuration

No additional configuration required. The implementation uses:
- Existing security configuration
- Existing database schema
- Existing JWT authentication
- Existing role hierarchy

## Next Steps

Consider implementing:
1. Email notifications for status changes
2. Document upload requirements
3. Application review comments/notes
4. Application statistics dashboard
5. Bulk operations for admins
