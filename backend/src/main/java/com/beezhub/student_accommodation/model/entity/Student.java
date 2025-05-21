package com.beezhub.student_accommodation.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

@Entity(name = "student")
@Data
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "student_number", unique = true, nullable = false)
    private String studentNumber;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name="date_of_birth")
    private Date dateOfBirth;
    @Column(name = "registration_date")
    private Date registrationDate;
    @Column(name = "gender")
    private String gender;
    @Column(name = "special_requirements")
    private String specialRequirements;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private AppUser appUser;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", referencedColumnName = "id")
    private Institution institution;

    @OneToOne(fetch = FetchType.EAGER)
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
