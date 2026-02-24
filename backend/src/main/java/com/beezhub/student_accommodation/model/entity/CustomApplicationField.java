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
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * A custom field defined by a building owner for their application form.
 * Students answer these fields when applying to the building.
 *
 * <p>Soft-delete via {@code isActive = false} preserves existing {@link CustomFieldAnswer} rows
 * per [REQ BO-004] edge case.</p>
 *
 * <p>Per [ARCH 5.1.1 rule 9]: tenant-scoped via Hibernate tenantFilter.</p>
 */
@Entity
@Table(name = "custom_application_field")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@jakarta.persistence.EntityListeners(com.beezhub.student_accommodation.model.listener.TenantListener.class)
public class CustomApplicationField implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    /**
     * One of: TEXT, NUMBER, DROPDOWN, CHECKBOX, DATE
     */
    @Column(name = "field_type", nullable = false, length = 20)
    private String fieldType;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "help_text", length = 500)
    private String helpText;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * JSON array of dropdown options, e.g. {@code ["Option A","Option B"]}.
     * Only relevant when {@code fieldType = DROPDOWN}.
     */
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
