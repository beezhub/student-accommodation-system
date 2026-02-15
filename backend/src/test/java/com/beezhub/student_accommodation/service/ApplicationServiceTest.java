package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.dto.ApplicationRequest;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.ApplicationStatusUpdateRequest;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.service.application.ApplicationAdminService;
import com.beezhub.student_accommodation.service.application.ApplicationCoreService;
import com.beezhub.student_accommodation.service.application.ApplicationDocumentService;
import com.beezhub.student_accommodation.service.application.ApplicationQueryService;
import com.beezhub.student_accommodation.service.application.ApplicationSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test for ApplicationService facade.
 * Verifies that the facade correctly delegates to decomposed services.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

        @Mock
        private ApplicationCoreService coreService;
        @Mock
        private ApplicationSearchService searchService;
        @Mock
        private ApplicationAdminService adminService;
        @Mock
        private ApplicationDocumentService documentService;
        @Mock
        private ApplicationQueryService queryService;

        @InjectMocks
        private ApplicationService applicationService;

        private ApplicationRequest applicationRequest;
        private ApplicationResponse applicationResponse;

        @BeforeEach
        void setUp() {
                applicationRequest = new ApplicationRequest();
                applicationRequest.setStudentId(1L);
                applicationRequest.setRequiresAccessibility(false);
                applicationRequest.setCourseName("Computer Science");

                applicationResponse = new ApplicationResponse();
                applicationResponse.setId(1L);
                applicationResponse.setApplicationCode("APP-2024-001");
        }

        @Test
        void submitApplication_shouldDelegateToCoreService() {
                when(coreService.submitApplication(applicationRequest)).thenReturn(applicationResponse);

                ApplicationResponse response = applicationService.submitApplication(applicationRequest);

                assertThat(response).isEqualTo(applicationResponse);
                verify(coreService).submitApplication(applicationRequest);
        }

        @Test
        void getApplicationById_shouldDelegateToCoreService() {
                when(coreService.getApplicationById(1L)).thenReturn(applicationResponse);

                ApplicationResponse response = applicationService.getApplicationById(1L);

                assertThat(response).isEqualTo(applicationResponse);
                verify(coreService).getApplicationById(1L);
        }

        @Test
        void getApplicationsByStudentId_shouldDelegateToCoreService() {
                List<ApplicationResponse> expectedList = Arrays.asList(applicationResponse);
                when(coreService.getApplicationsByStudentId(1L)).thenReturn(expectedList);

                List<ApplicationResponse> response = applicationService.getApplicationsByStudentId(1L);

                assertThat(response).isEqualTo(expectedList);
                verify(coreService).getApplicationsByStudentId(1L);
        }

        @Test
        void cancelApplication_shouldDelegateToCoreService() {
                doNothing().when(coreService).cancelApplication(1L);

                applicationService.cancelApplication(1L);

                verify(coreService).cancelApplication(1L);
        }

        @Test
        void getMyApplications_shouldDelegateToCoreService() {
                List<ApplicationResponse> expectedList = Arrays.asList(applicationResponse);
                when(coreService.getMyApplications()).thenReturn(expectedList);

                List<ApplicationResponse> response = applicationService.getMyApplications();

                assertThat(response).isEqualTo(expectedList);
                verify(coreService).getMyApplications();
        }

        @Test
        void getAllApplications_shouldDelegateToSearchService() {
                Pageable pageable = PageRequest.of(0, 20);
                PageResponse<ApplicationResponse> expectedPage = new PageResponse<>();
                when(searchService.getAllApplications(pageable)).thenReturn(expectedPage);

                PageResponse<ApplicationResponse> response = applicationService.getAllApplications(pageable);

                assertThat(response).isEqualTo(expectedPage);
                verify(searchService).getAllApplications(pageable);
        }

        @Test
        void getApplicationsByStatus_shouldDelegateToSearchService() {
                List<ApplicationResponse> expectedList = Arrays.asList(applicationResponse);
                when(searchService.getApplicationsByStatus("PENDING")).thenReturn(expectedList);

                List<ApplicationResponse> response = applicationService.getApplicationsByStatus("PENDING");

                assertThat(response).isEqualTo(expectedList);
                verify(searchService).getApplicationsByStatus("PENDING");
        }

        @Test
        void updateApplicationStatus_shouldDelegateToAdminService() {
                ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
                request.setStatus(ApplicationStatus.APPROVED.name());
                when(adminService.updateApplicationStatus(1L, request)).thenReturn(applicationResponse);

                ApplicationResponse response = applicationService.updateApplicationStatus(1L, request);

                assertThat(response).isEqualTo(applicationResponse);
                verify(adminService).updateApplicationStatus(1L, request);
        }

        @Test
        void markUnderReview_shouldDelegateToAdminService() {
                when(adminService.markUnderReview(1L)).thenReturn(applicationResponse);

                ApplicationResponse response = applicationService.markUnderReview(1L);

                assertThat(response).isEqualTo(applicationResponse);
                verify(adminService).markUnderReview(1L);
        }

        @Test
        void requestChanges_shouldDelegateToAdminService() {
                String reason = "Missing documents";
                when(adminService.requestChanges(1L, reason)).thenReturn(applicationResponse);

                ApplicationResponse response = applicationService.requestChanges(1L, reason);

                assertThat(response).isEqualTo(applicationResponse);
                verify(adminService).requestChanges(1L, reason);
        }

}
