package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentExpiryResponse;
import com.beezhub.student_accommodation.service.document.DocumentExpiryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expiring documents for the student's own documents page (GH-498). Kept apart from
 * {@code DocumentController}, which is already over the controller size limit.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document upload and management APIs")
public class DocumentExpiryController {

    private final DocumentExpiryQueryService documentExpiryQueryService;

    @GetMapping("/my/expiry")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "When my documents expire",
            description = "The calling student's expiring documents, with the date each expires and whether "
                    + "the new one can be uploaded now. Not paginated: one entry per expiring document type.")
    public ResponseEntity<List<DocumentExpiryResponse>> getMyDocumentExpiries() {
        return ResponseEntity.ok(documentExpiryQueryService.getMyDocumentExpiries());
    }
}
