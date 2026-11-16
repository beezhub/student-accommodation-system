package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.config.SecurityConfig;
import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.security.MustChangePasswordFilter;
import com.beezhub.student_accommodation.security.RateLimitingFilter;
import com.beezhub.student_accommodation.security.jwt.JwtAuthenticationFilter;
import com.beezhub.student_accommodation.security.user.AppUserDetailsService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import com.beezhub.student_accommodation.service.document.StudentDocumentTypeService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GH-443: {@code GET /api/document-type/my} is the student's view of the document types they can
 * upload, including companies' own required types. It is about the caller, so only a student may
 * ask; admins manage types through the tenant-scoped endpoints.
 */
@WebMvcTest(DocumentTypeController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@DisplayName("DocumentTypeController: the student's uploadable types")
class DocumentTypeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean DocumentTypeService documentTypeService;
    @MockitoBean StudentDocumentTypeService studentDocumentTypeService;
    @MockitoBean AppUserDetailsService appUserDetailsService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean RateLimitingFilter rateLimitingFilter;
    @MockitoBean MustChangePasswordFilter mustChangePasswordFilter;

    /**
     * A bare mocked filter never calls the chain, so nothing behind it would run. Pass through, so
     * the URL rules and @PreAuthorize are what decide; see WaitlistControllerTest.
     */
    @BeforeEach
    void passThroughMockedFilters() throws Exception {
        for (Filter filter : List.<Filter>of(jwtAuthenticationFilter, rateLimitingFilter, mustChangePasswordFilter)) {
            doAnswer(invocation -> {
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(filter).doFilter(any(), any(), any());
        }
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyDocumentTypes_studentWithBuildings_passesThemAndReturnsTypes() throws Exception {
        DocumentTypeResponse bursary = new DocumentTypeResponse();
        bursary.setId("50");
        bursary.setName("Bursary Letter");
        bursary.setIsRequired(true);
        when(studentDocumentTypeService.getUploadableTypes(List.of(3L, 7L))).thenReturn(List.of(bursary));

        mockMvc.perform(get("/api/document-type/my").param("buildingIds", "3,7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("50"))
                .andExpect(jsonPath("$[0].name").value("Bursary Letter"))
                .andExpect(jsonPath("$[0].isRequired").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyDocumentTypes_studentWithoutBuildings_asksWithNone() throws Exception {
        when(studentDocumentTypeService.getUploadableTypes(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/document-type/my"))
                .andExpect(status().isOk());

        verify(studentDocumentTypeService).getUploadableTypes(isNull());
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void getMyDocumentTypes_tenantAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/document-type/my").param("buildingIds", "3"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentDocumentTypeService);
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void getMyDocumentTypes_platformAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/document-type/my"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentDocumentTypeService);
    }

    @Test
    void getMyDocumentTypes_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/document-type/my"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(studentDocumentTypeService);
    }
}
