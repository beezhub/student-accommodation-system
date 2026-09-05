package com.beezhub.student_accommodation.model.entity;

import com.beezhub.student_accommodation.model.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * A student's uploaded document.
 *
 * <p>Documents are <strong>student-owned, not tenant-owned</strong>: one pack of documents is
 * reused across every application a student makes, including applications to different tenants.
 * The entity therefore carries no {@code tenantFilter} — filtering by the uploader's tenant hid
 * every student upload from tenant admins, because students are platform-tenant users.
 * {@code tenant_id} stays as the platform tenant for schema compatibility and is not a scope.
 * Tenant scoping for admins is explicit instead: see {@code DocumentAccessGuard}, which grants a
 * tenant admin access only to students who have an application in that admin's tenant.
 */
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Version
    private Long version;
    @Column(name = "document_name", nullable = false)
    private String documentName;
    @Column(name = "document_path", nullable = false)
    private String documentPath;
    @Column(name = "file_size")
    private Long fileSize; // File size in bytes
    @Column(name ="document_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id", nullable = false)
    private Student student;

    @OneToOne
    @JoinColumn(name = "document_type_id", referencedColumnName = "id", nullable = false)
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }



}
