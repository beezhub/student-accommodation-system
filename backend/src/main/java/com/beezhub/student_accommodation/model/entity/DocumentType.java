package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_type")
@Data
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @Column(name = "type_name", nullable = false)
    private String typeName;
    @Column(name = "description")
    private String description;
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;
    /**
     * Expires at the end of the company's academic year, for the student's institution, that the
     * document was uploaded in (GH-498). The date is worked out per company, not stored.
     */
    @Column(name = "is_expiring", nullable = false)
    private Boolean isExpiring = false;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
