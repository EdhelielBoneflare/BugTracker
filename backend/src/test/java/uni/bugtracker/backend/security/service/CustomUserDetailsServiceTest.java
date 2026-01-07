package uni.bugtracker.backend.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.security.CustomUserDetails;
import uni.bugtracker.backend.security.model.Role;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private DeveloperRepository developerRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        // Given
        String username = "john.doe";
        Developer developer = Developer.builder()
                .id("dev-123")
                .username(username)
                .password("encoded-pass")
                .role(Role.DEVELOPER)
                .build();

        when(developerRepository.findByUsername(username))
                .thenReturn(Optional.of(developer));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Then
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo("encoded-pass");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DEVELOPER");
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserNotFound() {
        // Given
        String username = "non.existent";
        when(developerRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void loadUserByUsername_shouldWorkWithDifferentRoles() {
        // Given
        Developer admin = Developer.builder()
                .id("admin-1")
                .username("admin")
                .password("pass")
                .role(Role.ADMIN)
                .build();

        when(developerRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Then
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}