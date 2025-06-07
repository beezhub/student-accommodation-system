package com.beezhub.student_accommodation.controller;

import com.beezhub.student_accommodation.model.dto.DocumentResponse;
import com.beezhub.student_accommodation.service.DocumentService;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload-multiple",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json"
    )
    public ResponseEntity<List<DocumentResponse>> uploadDocuments(@RequestParam("files") List<MultipartFile> files,
                                                  @RequestParam("documentTypes") List<Long> documentType,
                                                  HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.uploadDocuments(files, username, documentType));
    }

    @PostMapping(value = "/upload-single",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "application/json"
    )
    public String uploadSingleDocument(@RequestParam("file") MultipartFile file,
                                       @RequestParam("documentType") Long documentTypeId,
                                       HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        documentService.uploadSingleDocument(file, username, documentTypeId);
        return "File uploaded successfully";
    }

    @GetMapping("/info/{documentId}")
    public ResponseEntity<DocumentResponse> getDocumentUrl(@PathVariable Long documentId, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok().body(documentService.getDocumentInfo(documentId, username));
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        var documentDownloadResponse = documentService.downloadDocument(documentId, username);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + documentDownloadResponse.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(documentDownloadResponse.getContentType()))
                .body(documentDownloadResponse.getResource());
    }

    @GetMapping("/info/all/{userId}")
    public ResponseEntity<List<DocumentResponse>> getAllDocumentsByUserId(@PathVariable Long userId, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.getAllDocumentsByUserId(userId, username));
    }

    @PutMapping("/update-status/{documentId}")
    public ResponseEntity<DocumentResponse> updateDocumentStatus(@PathVariable Long documentId,
                                                                 @RequestParam String status,
                                                                 HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(documentService.updateDocumentStatus(documentId, status, username));
    }

    @DeleteMapping("/delete/{documentId}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long documentId, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        documentService.deleteDocument(documentId, username);
        return ResponseEntity.ok("Document deleted successfully");
    }
}
