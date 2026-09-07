package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "student")
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_number", unique = true, nullable = true)
    private String studentNumber;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Temporal(TemporalType.DATE)
    @Column(name="date_of_birth")
    private Date dateOfBirth;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "registration_date")
    private Date registrationDate;
    @Column(name = "gender")
    private String gender;
    @Column(name = "special_requirements")
    private String specialRequirements;

    // GH-239: who to call if something happens to this student. Nullable — existing students have
    // no value and none can be invented for them; ApplicationCoreService.submitApplication is what
    // makes it required going forward. UserAnonymizationService must clear all four: the contact is
    // a third party who never signed up, and leaving their name and number behind after a student
    // is anonymised is precisely the data anonymisation exists to remove.
    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_email")
    private String emergencyContactEmail;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", referencedColumnName = "id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "year_of_study_id", referencedColumnName = "id")
    private YearOfStudy yearOfStudy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        registrationDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
