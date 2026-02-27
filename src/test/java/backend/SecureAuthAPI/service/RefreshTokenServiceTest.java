package backend.SecureAuthAPI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import backend.secureauthapi.model.RefreshToken;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.RefreshTokenRepository;
import backend.secureauthapi.security.util.RefreshTokenGenerator;
import backend.secureauthapi.security.util.RefreshTokenHasher;
import backend.secureauthapi.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    @Mock private RefreshTokenGenerator generator;
    @Mock private RefreshTokenHasher hasher;
    @Mock private Clock clock;

    @InjectMocks private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L);
    }

    @Nested
    @DisplayName("Issue Refresh Token Tests")
    class IssueRefreshTokenTests {

        @Test
        @DisplayName("Should generate, hash, persist and return the raw token")
        void issueRefreshToken_shouldReturnRawToken_andPersistHashedToken() {

            // Given
            User user = createSavedUser();
            String deviceInfo = "Chrome/Windows";
            String ipAddress = "192.168.1.1";

            String rawToken = "raw-uuid-token-value";
            String tokenHash = "hashed-token-value";

            Instant fixedNow = Instant.parse("2026-02-26T18:00:00Z");

            when(clock.instant()).thenReturn(fixedNow);
            when(generator.generateRefreshToken()).thenReturn(rawToken);
            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);

            // When
            String result = refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress);

            // Then
            assertThat(result).isEqualTo(rawToken);
            assertThat(result).isNotEqualTo(tokenHash);

            // Verify
            verify(generator).generateRefreshToken();
            verify(hasher).hash(eq(rawToken));
            verify(repository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("Revoke Token Tests")
    class RevokeTokenTests {

        @Test
        @DisplayName("Should revoke token when it exists and is active")
        void revokeToken_shouldRevokeToken_whenTokenExists() {

            // Given
            String rawToken = "valid-raw-token";
            String tokenHash = "valid-token-hash";

            User user = createSavedUser();
            RefreshToken refreshToken = createActiveToken(user, tokenHash);

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);

            when(repository.findByTokenHashAndRevokedFalse(eq(tokenHash)))
                    .thenReturn(Optional.of(refreshToken));

            // When
            refreshTokenService.revokeToken(rawToken);

            // Then
            assertThat(refreshToken.isRevoked()).isTrue();

            // Verify
            verify(hasher).hash(eq(rawToken));
            verify(repository).findByTokenHashAndRevokedFalse(eq(tokenHash));
            verify(repository).save(eq(refreshToken));
        }

        @Test
        @DisplayName("Should do nothing when token does not exist or is already revoked")
        void revokeToken_shouldDoNothing_whenTokenNotFound() {

            // Given
            String rawToken = "nonexistent-or-revoked-token";
            String tokenHash = "nonexistent-token-hash";

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);

            when(repository.findByTokenHashAndRevokedFalse(eq(tokenHash)))
                    .thenReturn(Optional.empty());

            // When
            refreshTokenService.revokeToken(rawToken);

            // Verify
            verify(hasher).hash(eq(rawToken));
            verify(repository).findByTokenHashAndRevokedFalse(eq(tokenHash));
            verify(repository, never()).save(any());
        }
    }

    private User createSavedUser() {
        User user = new User("John Doe", "john@example.com", "encodedPassword", Role.USER);
        user.setId(1L);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private RefreshToken createActiveToken(User user, String tokenHash) {
        Instant expiresAt = Instant.now().plusSeconds(604800);
        return new RefreshToken(tokenHash, user, expiresAt);
    }
}
