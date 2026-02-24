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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Records a student's answer to a {@link CustomApplicationField} for a given application.
 *
 * <p>Answers are retained even when the parent {@link CustomApplicationField} is soft-deleted
 * ({@code is_active = false}) to preserve historical application data per [REQ BO-004].</p>
 */
@Entity
@Table(name = "custom_field_answer",
        uniqueConstraints = @UniqueConstraint(name = "uq_field_answer",
                columnNames = {"application_id", "field_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private CustomApplicationField field;

    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        answeredAt = LocalDateTime.now();
    }
}
