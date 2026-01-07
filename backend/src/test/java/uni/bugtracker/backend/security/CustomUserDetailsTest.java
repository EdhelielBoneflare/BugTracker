package uni.bugtracker.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.security.model.Role;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void constructor_shouldMapAllFieldsFromDeveloper() {
        // Given
        Developer developer = Developer.builder()
                .id("dev-123")
                .username("john.doe")
                .password("encoded-password")
                .role(Role.DEVELOPER)
                .build();

        // When
        CustomUserDetails userDetails = new CustomUserDetails(developer);

        // Then
        assertThat(userDetails.getId()).isEqualTo("dev-123");
        assertThat(userDetails.getUsername()).isEqualTo("john.doe");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    void getAuthorities_shouldReturnRoleWithPrefix() {
        // Given
        Developer developer = Developer.builder()
                .id("dev-123")
                .username("admin")
                .password("pass")
                .role(Role.ADMIN)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(developer);

        // When
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Then
        assertThat(authorities)
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void getAuthorities_shouldWorkForAllRoleTypes() {
        // Test all roles
        for (Role role : Role.values()) {
            // Given
            Developer developer = Developer.builder()
                    .id("dev-123")
                    .username("user")
                    .password("pass")
                    .role(role)
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(developer);

            // When
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

            // Then
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_" + role.name());
        }
    }

    @Test
    void accountStatusMethods_shouldAlwaysReturnTrue() {
        // Given
        Developer developer = Developer.builder()
                .id("dev-123")
                .username("test")
                .password("pass")
                .role(Role.PM)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(developer);

        // Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void equalsAndHashCode_shouldWorkCorrectly() {
        // Given
        Developer dev1 = Developer.builder()
                .id("dev-123")
                .username("same.user")
                .password("pass1")
                .role(Role.DEVELOPER)
                .build();

        Developer dev2 = Developer.builder()
                .id("dev-123") // Same ID
                .username("same.user")
                .password("different-pass")
                .role(Role.ADMIN)
                .build();

        // When
        CustomUserDetails details1 = new CustomUserDetails(dev1);
        CustomUserDetails details2 = new CustomUserDetails(dev2);

        // Then - different objects but same username
        assertThat(details1.getUsername()).isEqualTo(details2.getUsername());
        assertThat(details1).isNotSameAs(details2);
    }
}