package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.RoomAllocationNotFoundException;
import com.beezhub.student_accommodation.model.dto.RoomAllocationRequest;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Document;
import com.beezhub.student_accommodation.model.entity.DocumentType;
import com.beezhub.student_accommodation.model.entity.Room;
import com.beezhub.student_accommodation.model.entity.RoomAllocation;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.RoomAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationDocumentCoordinatorTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private RoomAllocationService roomAllocationService;
    @Mock
    private RoomAllocationRepository roomAllocationRepository;
    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationDocumentCoordinator coordinator;

    private Student student;
    private Application application;
    private DocumentType requiredDocType;
    private DocumentType optionalDocType;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(10L);

        application = new Application();
        application.setId(100L);
        application.setStudent(student);

        requiredDocType = new DocumentType();
        requiredDocType.setIsRequired(true);
        requiredDocType.setId(1L);

        optionalDocType = new DocumentType();
        optionalDocType.setIsRequired(false);
        optionalDocType.setId(2L);
    }

    @Test
    void tryAllocateOrQueue_notAllRequiredApproved_doesNothing() {
        Document doc1 = Document.builder().documentType(requiredDocType).documentStatus(DocumentStatus.PENDING).build();
        when(documentRepository.findByApplicationId(100L)).thenReturn(List.of(doc1));

        coordinator.tryAllocateOrQueue(student, application);

        verify(roomAllocationService, never()).findAvailableRoom();
        verify(roomAllocationService, never()).createRoomAllocation(any());
        verify(roomAllocationService, never()).enqueueWaitingList(anyLong(), anyLong());
    }

    @Test
    void tryAllocateOrQueue_allRequiredApproved_roomAvailable_allocates() {
        Document doc1 = Document.builder().documentType(requiredDocType).documentStatus(DocumentStatus.APPROVED).build();
        when(documentRepository.findByApplicationId(100L)).thenReturn(List.of(doc1));

        Room room = new Room();
        room.setId(5L);
        room.setRoomNumber("101");
        when(roomAllocationService.findAvailableRoom()).thenReturn(room);

        coordinator.tryAllocateOrQueue(student, application);

        verify(roomAllocationService).createRoomAllocation(any(RoomAllocationRequest.class));
    }

    @Test
    void tryAllocateOrQueue_allRequiredApproved_noRoom_queues() {
        Document doc1 = Document.builder().documentType(requiredDocType).documentStatus(DocumentStatus.APPROVED).build();
        when(documentRepository.findByApplicationId(100L)).thenReturn(List.of(doc1));

        when(roomAllocationService.findAvailableRoom()).thenReturn(null);

        coordinator.tryAllocateOrQueue(student, application);

        verify(roomAllocationService).enqueueWaitingList(student.getId(), application.getId());
    }

    @Test
    void tryAllocateOrQueue_allocationThrows_logsError() {
        Document doc1 = Document.builder().documentType(requiredDocType).documentStatus(DocumentStatus.APPROVED).build();
        when(documentRepository.findByApplicationId(100L)).thenReturn(List.of(doc1));

        Room room = new Room();
        room.setId(5L);
        when(roomAllocationService.findAvailableRoom()).thenReturn(room);
        
        doThrow(new RoomAllocationNotFoundException("Fail")).when(roomAllocationService).createRoomAllocation(any());

        assertDoesNotThrow(() -> coordinator.tryAllocateOrQueue(student, application));
    }
    
    @Test
    void findActiveAllocation_delegatesToRepository() {
        coordinator.findActiveAllocation(10L);
        verify(roomAllocationRepository).findActiveAllocationByStudentId(10L);
    }
    
    @Test
    void findApprovedApplication_success() {
        Application approvedApp = new Application();
        approvedApp.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findByStudent_Id(10L)).thenReturn(List.of(approvedApp));
        
        Application result = coordinator.findApprovedApplication(10L);
        assertNotNull(result);
        assertEquals(ApplicationStatus.APPROVED, result.getStatus());
    }
}
