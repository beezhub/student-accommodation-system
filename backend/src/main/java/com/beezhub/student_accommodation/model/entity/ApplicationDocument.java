package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Junction table linking student documents to applications.
 *
 * <p>Introduced in TASK-2.01 to decouple {@link Document} from {@link Application}.
 * A single document (e.g., student ID) can be attached to multiple applications
 * across different building-owner tenants. Once verified on any application it is
 * trusted platform-wide.</p>
 *
 * <p>This entity is intentionally NOT tenant-filtered: access is controlled by
 * ownership — a student can see their own, a building owner can see documents
 * attached to their building's applications.</p>
 */
@Entity
@Table(name = "application_document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "attached_at", nullable = false, updatable = false)
    private LocalDateTime attachedAt;

    @PrePersist
    protected void onCreate() {
        attachedAt = LocalDateTime.now();
    }
}
