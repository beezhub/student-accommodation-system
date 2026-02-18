package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.beezhub.student_accommodation.model.enums.DocumentStatus.APPROVED;

import com.beezhub.student_accommodation.exceptions.ApplicationNotApprovedException;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationDocumentCoordinator {

    private final DocumentRepository documentRepository;
    private final RoomAllocationService roomAllocationService;
    private final RoomAllocationRepository roomAllocationRepository;
    private final ApplicationRepository applicationRepository;

    public Application findApprovedApplication(Long studentId) {
        return applicationRepository.findByStudent_Id(studentId)
                .stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPROVED)
                .findFirst()
                .orElseThrow(() -> new ApplicationNotApprovedException(
                        "Documents can only be reviewed for approved applications"));
    }

    @Transactional
    public void tryAllocateOrQueue(Student student, Application application) {
        // Check if all required docs are approved for this application
        List<Document> documents = documentRepository.findByApplicationId(application.getId());
        long requiredApproved = documents.stream()
                .filter(doc -> doc.getDocumentStatus() == APPROVED)
                .map(Document::getDocumentType)
                .filter(DocumentType::getIsRequired)
                .count();
        long requiredTotal = documents.stream()
                .map(Document::getDocumentType)
                .filter(DocumentType::getIsRequired)
                .count();

        if (requiredApproved < requiredTotal) {
            log.info("Student {} does not have all required documents approved yet", student.getId());
            return;
        }

        try {
            Room availableRoom = roomAllocationService.findAvailableRoom();
            if (availableRoom != null) {
                RoomAllocationRequest request = new RoomAllocationRequest();
                request.setStudentId(student.getId());
                request.setRoomId(availableRoom.getId());
                request.setApplicationId(application.getId());
                roomAllocationService.createRoomAllocation(request);
                log.info("Allocated room {} to student {} after document approvals", availableRoom.getRoomNumber(),
                        student.getId());
            } else {
                roomAllocationService.enqueueWaitingList(student.getId(), application.getId());
            }
        } catch (com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException |
                 com.beezhub.student_accommodation.exceptions.RoomCapacityExceededException |
                 com.beezhub.student_accommodation.exceptions.StudentAlreadyAllocatedException e) {
            log.error("Failed to auto-allocate room for student {}", student.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public RoomAllocation findActiveAllocation(Long studentId) {
        return roomAllocationRepository.findActiveAllocationByStudentId(studentId).orElse(null);
    }
}
