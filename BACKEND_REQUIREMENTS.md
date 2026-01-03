# Backend Implementation Requirements

## Document Verification & Room Allocation Workflow

### Overview
The document verification system requires backend validation and automatic room allocation when all documents are approved for students with approved applications.

---

## 1. Document Status Update Endpoint

### Endpoint
```
PUT /api/documents/update-status/{documentId}?status={STATUS}
```

### Required Validations

#### Step 1: Check Application Status
```java
// Before updating document status, verify:
1. Get the student associated with the document
2. Get the student's application
3. Validate application status is "APPROVED"
4. If not approved: throw BadRequestException("Documents can only be reviewed for approved applications")
```

#### Step 2: Update Document Status
```java
// If validation passes:
1. Update document status to the requested status
2. Save document
```

#### Step 3: Check for Room Allocation (if status is APPROVED)
```java
// If new status is "APPROVED":
1. Get all documents for the student
2. Check if ALL required documents are now "APPROVED"
3. If yes: Trigger automatic room allocation
```

### Response Structure

#### Success Response with Room Allocation
```json
{
  "id": 46,
  "documentName": "Student ID",
  "documentStatus": "APPROVED",
  "documentTypeName": "Identity",
  "studentId": 123,
  "studentFirstName": "John",
  "studentLastName": "Doe",
  "studentEmail": "john.doe@example.com",
  "applicationStatus": "APPROVED",
  "applicationId": 456,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T14:45:00",
  "roomAllocated": true,      // NEW: Indicates room was allocated
  "roomNumber": "A-101"        // NEW: The allocated room number
}
```

#### Success Response without Room Allocation
```json
{
  "id": 46,
  "documentStatus": "APPROVED",
  // ... other fields
  "roomAllocated": false
}
```

#### Error Response (Application Not Approved)
```json
{
  "message": "Documents can only be reviewed for approved applications",
  "error": "Application not approved",
  "status": 400,
  "timestamp": "2024-01-15T14:45:00"
}
```

---

## 2. Room Allocation Logic

### Required Document Types
Define which document types are required before room allocation:
```java
List<String> REQUIRED_DOCUMENT_TYPES = Arrays.asList(
    "Identity",           // Student ID, Passport, etc.
    "Academic",           // Proof of enrollment
    "Financial",          // Proof of payment capability (optional?)
    "Medical"            // Medical certificate (optional?)
);
```

### Allocation Process
```java
public void checkAndAllocateRoom(Long studentId) {
    // 1. Get student's application
    Application application = getStudentApplication(studentId);

    // 2. Verify application is approved
    if (!application.getStatus().equals("APPROVED")) {
        return; // Cannot allocate room for non-approved applications
    }

    // 3. Check if room already allocated
    if (application.getRoomId() != null) {
        return; // Room already allocated
    }

    // 4. Get all documents for student
    List<Document> documents = documentRepository.findByStudentId(studentId);

    // 5. Check if all required document types are approved
    Set<String> approvedTypes = documents.stream()
        .filter(doc -> doc.getStatus().equals("APPROVED"))
        .map(doc -> doc.getDocumentType().getName())
        .collect(Collectors.toSet());

    boolean allRequiredDocsApproved = approvedTypes.containsAll(REQUIRED_DOCUMENT_TYPES);

    // 6. If all documents approved, allocate room
    if (allRequiredDocsApproved) {
        Room availableRoom = findAvailableRoom();
        if (availableRoom != null) {
            // Allocate room
            application.setRoom(availableRoom);
            application.setRoomAllocationDate(LocalDateTime.now());
            applicationRepository.save(application);

            // Update room status
            availableRoom.setStatus("OCCUPIED");
            availableRoom.setOccupiedBy(student);
            roomRepository.save(availableRoom);

            // Send notification to student (email/SMS)
            notificationService.sendRoomAllocationNotification(student, availableRoom);
        }
    }
}
```

### Room Selection Strategy
```java
public Room findAvailableRoom() {
    // Strategy options:
    // 1. First available room
    // 2. Room based on student preferences (gender, building, floor)
    // 3. Room based on payment tier
    // 4. Random allocation

    return roomRepository.findFirstByStatusOrderByBuildingAscFloorAscRoomNumberAsc("AVAILABLE");
}
```

---

## 3. Document Search Endpoint Enhancement

### Current Endpoint
```
GET /api/documents/search?studentName=&documentType=&status=&page=0&size=20
```

### Required Enhancement
Add `applicationStatus` parameter to filter documents by application status:

```
GET /api/documents/search?applicationStatus=APPROVED&page=0&size=20
```

### Implementation
```java
@GetMapping("/search")
public Page<DocumentResponse> searchDocuments(
    @RequestParam(required = false) String studentName,
    @RequestParam(required = false) String documentType,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String applicationStatus,  // NEW
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    // Build query with application status filter
    Specification<Document> spec = DocumentSpecification.build(
        studentName,
        documentType,
        status,
        applicationStatus  // NEW
    );

    return documentRepository.findAll(spec, PageRequest.of(page, size))
        .map(this::toDocumentResponse);
}
```

### DocumentResponse Must Include
```java
public class DocumentResponse {
    // ... existing fields
    private String applicationStatus;  // NEW: Status of student's application
    private Long applicationId;        // NEW: ID of student's application
}
```

---

## 4. Transaction Management

### Important: Use @Transactional
```java
@Transactional
public DocumentResponse updateDocumentStatus(Long documentId, String status) {
    // All validation, update, and room allocation should happen in one transaction
    // This ensures data consistency
}
```

---

## 5. Notification Service (Recommended)

### Email Notification Template
```
Subject: Room Allocated - Welcome to [Building Name]

Dear [Student Name],

Congratulations! Your accommodation has been approved and a room has been allocated to you.

Room Details:
- Building: [Building Name]
- Room Number: [Room Number]
- Floor: [Floor Number]
- Check-in Date: [Date]

Please visit the accommodation office to collect your keys and complete the check-in process.

If you have any questions, please contact us at accommodations@university.edu

Best regards,
Student Accommodation Team
```

---

## 6. Database Schema Updates

### Application Table
Ensure the Application table has:
```sql
ALTER TABLE applications ADD COLUMN room_id BIGINT;
ALTER TABLE applications ADD COLUMN room_allocation_date TIMESTAMP;
ALTER TABLE applications ADD CONSTRAINT fk_application_room
    FOREIGN KEY (room_id) REFERENCES rooms(id);
```

### Document Table
Ensure proper foreign key to student/application:
```sql
ALTER TABLE documents ADD COLUMN student_id BIGINT;
ALTER TABLE documents ADD CONSTRAINT fk_document_student
    FOREIGN KEY (student_id) REFERENCES students(id);
```

---

## 7. Testing Checklist

### Unit Tests
- [ ] Test document status validation (application not approved)
- [ ] Test document status update (application approved)
- [ ] Test room allocation logic (all docs approved)
- [ ] Test room allocation logic (not all docs approved)
- [ ] Test room already allocated scenario
- [ ] Test no available rooms scenario

### Integration Tests
- [ ] Test complete workflow: application approval → document approval → room allocation
- [ ] Test bulk document approval
- [ ] Test concurrent document approvals for same student

### API Tests
- [ ] Test PUT /documents/update-status with approved application
- [ ] Test PUT /documents/update-status with pending application (should fail)
- [ ] Test GET /documents/search with applicationStatus filter

---

## 8. Error Handling

### Exception Types
```java
// Custom exceptions
public class ApplicationNotApprovedException extends RuntimeException {
    public ApplicationNotApprovedException(String message) {
        super(message);
    }
}

public class NoAvailableRoomsException extends RuntimeException {
    public NoAvailableRoomsException(String message) {
        super(message);
    }
}
```

### Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotApproved(
        ApplicationNotApprovedException ex
    ) {
        ErrorResponse error = new ErrorResponse(
            "Documents can only be reviewed for approved applications",
            400,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

---

## 9. Logging

### Add Logging for Audit Trail
```java
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentResponse updateDocumentStatus(Long documentId, String status) {
        logger.info("Updating document {} status to {}", documentId, status);

        // Validation
        logger.debug("Validating application status for document {}", documentId);

        // Update
        logger.info("Document {} status updated to {}", documentId, status);

        // Room allocation
        if (roomAllocated) {
            logger.info("Room {} allocated to student {} for document approval",
                roomNumber, studentId);
        }
    }
}
```

---

## 10. Security Considerations

### Authorization
```java
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/update-status/{documentId}")
public ResponseEntity<DocumentResponse> updateDocumentStatus(...) {
    // Only admins can update document status
}
```

### Prevent Race Conditions
```java
// Use optimistic locking
@Entity
@Table(name = "documents")
public class Document {
    @Version
    private Long version;  // Prevents concurrent updates
}
```

---

## Summary

The backend must handle:
1. ✅ Validate application status before allowing document review
2. ✅ Update document status
3. ✅ Check if all required documents are approved
4. ✅ Automatically allocate room when conditions are met
5. ✅ Return comprehensive response with room allocation info
6. ✅ Handle errors gracefully with clear messages
7. ✅ Send notifications to students
8. ✅ Maintain audit trail through logging

This ensures the frontend remains a thin client while all business logic is securely handled on the backend.
