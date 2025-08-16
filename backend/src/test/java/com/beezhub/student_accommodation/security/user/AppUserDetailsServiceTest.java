package com.beezhub.student_accommodation.security.user;

import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository repository;

    @InjectMocks
    private AppUserDetailsService service;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        AppUser user = new AppUser();
        user.setEmail("john@doe.com");
        user.setUserPassword("secret");
        user.setUserRole(UserRole.STUDENT);
        when(repository.findByEmail("john@doe.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("john@doe.com");

        assertThat(details.getUsername()).isEqualTo("john@doe.com");
        assertThat(details.getPassword()).isEqualTo("secret");
        assertThat(details.getAuthorities()).extracting("authority").contains("STUDENT");
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserMissing() {
        when(repository.findByEmail("missing@foo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@foo.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
