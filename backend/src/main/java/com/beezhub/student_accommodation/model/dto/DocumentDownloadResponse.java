package com.beezhub.student_accommodation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentDownloadResponse {
    private Resource resource;
    private String filename;
    private String contentType;
}
