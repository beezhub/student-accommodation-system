package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAnonymizationServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserAnonymizationService service;

    @BeforeEach
    void setUp() {
        service = new UserAnonymizationService(appUserRepository, studentRepository, passwordEncoder);
    }

    @Test
    void anonymizeUser_replacesPiiAndSaves() {
        // Arrange
        Long userId = 123L;
        AppUser user = AppUser.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .userRole(UserRole.STUDENT)
                .isAnonymized(false)
                .build();

        Student student = new Student();
        student.setStudentNumber("S12345");
        student.setPhoneNumber("555-0199");
        student.setSpecialRequirements("Vegan");
        // GH-239: someone else's name and phone number, held on this student's record.
        student.setEmergencyContactName("Thandi Mokoena");
        student.setEmergencyContactRelationship("Mother");
        student.setEmergencyContactPhone("+27 82 123 4567");
        student.setEmergencyContactEmail("thandi@example.com");

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-random-password");

        // Act
        service.anonymizeUser(userId);

        // Assert
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();

        assertEquals("Deleted", savedUser.getFirstName());
        assertEquals("User #123", savedUser.getLastName());
        assertEquals("deleted_123@anonymized.local", savedUser.getEmail());
        assertTrue(savedUser.getIsAnonymized());
        assertFalse(savedUser.getEmailVerified());
        assertEquals("hashed-random-password", savedUser.getUserPassword());

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();

        assertEquals("DEL-123", savedStudent.getStudentNumber());
        assertNull(savedStudent.getPhoneNumber());
        assertNull(savedStudent.getSpecialRequirements());
        // GH-239. The emergency contact is a third party who never signed up and never consented
        // to being in this system. Leaving them behind after the student has been anonymised keeps
        // precisely the data anonymisation exists to remove — the omission the issue calls the
        // single most likely one.
        assertNull(savedStudent.getEmergencyContactName());
        assertNull(savedStudent.getEmergencyContactRelationship());
        assertNull(savedStudent.getEmergencyContactPhone());
        assertNull(savedStudent.getEmergencyContactEmail());
    }

    @Test
    void anonymizeUser_alreadyAnonymized_doesNothing() {
        Long userId = 123L;
        AppUser user = AppUser.builder()
                .id(userId)
                .isAnonymized(true)
                .build();

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));

        service.anonymizeUser(userId);

        verify(appUserRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void anonymize_userCannotLogin() {
        // Arrange — a verified, active user
        Long userId = 456L;
        AppUser user = AppUser.builder()
                .id(userId)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .userRole(UserRole.STUDENT)
                .isAnonymized(false)
                .emailVerified(true)
                .build();

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.findByAppUser(user)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-random-hash");

        // Act
        service.anonymizeUser(userId);

        // Assert — saved user has properties that prevent login
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        // Email is scrambled — login by original email no longer works
        assertEquals("deleted_456@anonymized.local", saved.getEmail());
        // Email verification revoked — even if email were known, it cannot log in
        assertFalse(saved.getEmailVerified());
        // isAnonymized flag set — application layer can also gate on this
        assertTrue(saved.getIsAnonymized());
        // Password replaced with an unguessable random hash
        verify(passwordEncoder).encode(anyString());
        assertEquals("bcrypt-random-hash", saved.getUserPassword());
    }
}
