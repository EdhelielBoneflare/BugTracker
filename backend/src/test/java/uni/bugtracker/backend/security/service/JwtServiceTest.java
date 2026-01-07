package uni.bugtracker.backend.security.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import uni.bugtracker.backend.security.model.Role;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private final String username = "testuser";
    private final Role userRole = Role.DEVELOPER;

    @BeforeEach
    void setUp() {
        // Set the secret key
        String testSecretKey = "mySuperSecretKeyThatIsAtLeast32CharactersLong";
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3600000L); // 1 hour
    }

    private void setupUserDetailsMock() {
        when(userDetails.getUsername()).thenReturn(username);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority(userRole.name())))
                .when(userDetails).getAuthorities();
    }

    private SecretKey getSignInKey() {
        return ReflectionTestUtils.invokeMethod(jwtService, "getSignInKey");
    }

    @Test
    void generateToken_ShouldCreateValidTokenWithCorrectClaims() {
        // Given
        setupUserDetailsMock();

        // When
        String token = jwtService.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull().isNotBlank();

        String extractedUsername = jwtService.extractUsername(token);
        Role extractedRole = jwtService.getRoleFromToken(token);

        assertThat(extractedUsername).isEqualTo(username);
        assertThat(extractedRole).isEqualTo(userRole);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsernameFromToken() {
        // Given
        setupUserDetailsMock();
        String token = createValidToken();

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void isTokenValid_ShouldReturnTrueForValidTokenAndMatchingUser() {
        // Given
        setupUserDetailsMock();
        String token = createValidToken();

        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalseForDifferentUser() {
        // Given
        setupUserDetailsMock();
        String token = createValidToken();
        UserDetails differentUser = User.withUsername("differentUser")
                .password("password")
                .authorities(userRole.name())
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_ShouldReturnTrueForExpiredToken() {
        // Given
        setupUserDetailsMock();
        SecretKey key = getSignInKey();
        Date pastDate = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago
        String expiredToken = Jwts.builder()
                .subject(username)
                .claim("role", userRole.name())
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(pastDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // When & Then
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(jwtService, "isTokenExpired", expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void isTokenExpired_ShouldReturnFalseForValidToken() {
        // Given
        setupUserDetailsMock();
        String token = createValidToken();

        // When
        boolean isExpired = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(jwtService, "isTokenExpired", token));

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    void getRoleFromToken_ShouldReturnCorrectRole() {
        // Given
        setupUserDetailsMock();
        String token = createValidToken();

        // When
        Role extractedRole = jwtService.getRoleFromToken(token);

        // Then
        assertThat(extractedRole).isEqualTo(userRole);
    }

    @Test
    void getRoleFromToken_ShouldThrowExceptionWhenTokenHasNoRoleClaim() {
        // Given
        SecretKey key = getSignInKey();
        String tokenWithoutRole = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtService.getRoleFromToken(tokenWithoutRole))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractUsername_ShouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.string";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void extractUsername_ShouldThrowExceptionForTamperedToken() {
        // Given
        setupUserDetailsMock();
        String validToken = createValidToken();
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void extractUsername_ShouldThrowExceptionForExpiredToken() {
        // Given
        SecretKey key = getSignInKey();
        Date pastDate = new Date(System.currentTimeMillis() - 10000); // 10 seconds ago

        String expiredToken = Jwts.builder()
                .subject(username)
                .claim("role", userRole.name())
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(pastDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private String createValidToken() {
        return jwtService.generateToken(userDetails);
    }
}