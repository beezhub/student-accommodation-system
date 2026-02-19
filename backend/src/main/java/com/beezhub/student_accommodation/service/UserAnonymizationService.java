package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnonymizationService {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void anonymizeUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (Boolean.TRUE.equals(user.getIsAnonymized())) {
            log.warn("User {} is already anonymized", userId);
            return;
        }

        log.info("Anonymizing user {}", userId);

        // 1. Anonymize AppUser
        user.setFirstName("Deleted");
        user.setLastName("User #" + userId);
        user.setEmail("deleted_" + userId + "@anonymized.local");
        // Set an invalid password hash that cannot be matched
        user.setUserPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setIsAnonymized(true);
        user.setEmailVerified(false);
        user.setMustChangePassword(false);

        appUserRepository.save(user);

        // 2. Anonymize Student record if exists
        Optional<Student> studentOpt = studentRepository.findByAppUser(user);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            student.setStudentNumber("DEL-" + userId);
            student.setPhoneNumber(null);
            student.setDateOfBirth(null);
            student.setGender(null);
            student.setSpecialRequirements(null);
            studentRepository.save(student);
            log.info("Anonymized student record for user {}", userId);
        }

        log.info("Successfully anonymized user {}", userId);
    }
}
