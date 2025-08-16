package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentTypeResponse;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentTypeControllerTest {

    private MockMvc mockMvc;
    private DocumentTypeService service;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(DocumentTypeService.class);
        DocumentTypeController controller = new DocumentTypeController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllDocumentTypes_shouldReturnOkWithList() throws Exception {
        DocumentTypeResponse r1 = new DocumentTypeResponse();
        r1.setId("1");
        r1.setName("ID");
        DocumentTypeResponse r2 = new DocumentTypeResponse();
        r2.setId("2");
        r2.setName("Photo");
        when(service.getAllDocumentTypes()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/document-type").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ID"))
                .andExpect(jsonPath("$[1].name").value("Photo"));
    }

    @Test
    void getRequiredDocumentTypes_shouldReturnOkWithList() throws Exception {
        DocumentTypeResponse r1 = new DocumentTypeResponse();
        r1.setId("1");
        r1.setName("ID");
        when(service.getRequiredDocumentTypes()).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/document-type/required").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ID"));
    }
}
