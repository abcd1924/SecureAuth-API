package backend.SecureAuthAPI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import backend.secureauthapi.exception.token.InvalidRefreshTokenException;
import backend.secureauthapi.exception.token.RefreshTokenReuseException;
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

    @Nested
    @DisplayName("Revoke All Tokens By User Tests")
    class RevokeAllTokensByUserTests {

        @Test
        @DisplayName("Should revoke all active tokens for a user")
        void revokeAllTokensByUser_shouldRevokeAllActiveTokens_whenUserHasActiveTokens() {

            // Given
            User user = createSavedUser();
            String hash1 = "hash-token-1";
            String hash2 = "hash-token-2";

            RefreshToken token1 = createActiveToken(user, hash1);
            RefreshToken token2 = createActiveToken(user, hash2);
            List<RefreshToken> activeTokens = List.of(token1, token2);

            when(repository.findByUserIdAndRevokedFalse(eq(user.getId()))).thenReturn(activeTokens);

            // When
            refreshTokenService.revokeAllTokensByUser(user.getId());

            // Then
            assertThat(token1.isRevoked()).isTrue();
            assertThat(token2.isRevoked()).isTrue();

            // Verify
            verify(repository).findByUserIdAndRevokedFalse(eq(user.getId()));
            verify(repository).saveAll(eq(activeTokens));
        }

        @Test
        @DisplayName("Should do nothing when user has no active tokens")
        void revokeAllTokensByUser_shouldDoNothing_whenUserHasNoActiveTokens() {

            // Given
            Long userId = 99L;

            when(repository.findByUserIdAndRevokedFalse(eq(userId))).thenReturn(List.of());

            // When
            refreshTokenService.revokeAllTokensByUser(userId);

            // Verify
            verify(repository).findByUserIdAndRevokedFalse(eq(userId));
            verify(repository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Rotate And Issue Refresh Token Tests")
    class RotateAndIssueRefreshTokenTests {

        @Test
        @DisplayName("Should rotate old token and return a new raw token")
        void rotateAndIssueRefreshToken_shouldReturnNewToken_whenTokenIsValid() {

            // Given
            String oldRawToken = "old-raw-token";
            String oldTokenHash = "old-token-hash";

            User user = createSavedUser();
            RefreshToken existingToken = createActiveToken(user, oldTokenHash);
            existingToken.setDeviceInfo("Chrome/Windows");
            existingToken.setIpAddress("192.168.1.1");

            when(hasher.hash(eq(oldRawToken))).thenReturn(oldTokenHash);

            when(repository.findByTokenHash(eq(oldTokenHash)))
                    .thenReturn(Optional.of(existingToken));

            String newRawToken = "new-raw-token";
            String newTokenHash = "new-token-hash";
            Instant fixedNow = Instant.parse("2026-02-27T17:00:00Z");

            when(generator.generateRefreshToken()).thenReturn(newRawToken);
            when(hasher.hash(eq(newRawToken))).thenReturn(newTokenHash);
            when(clock.instant()).thenReturn(fixedNow);

            // When
            String result = refreshTokenService.rotateAndIssueRefreshToken(oldRawToken);

            // Then
            assertThat(result).isEqualTo(newRawToken);
            assertThat(result).isNotEqualTo(oldRawToken);
            assertThat(existingToken.isRevoked()).isTrue();
            assertThat(existingToken.getRotatedAt()).isNotNull();

            // Verify
            verify(hasher).hash(eq(oldRawToken));
            verify(repository).findByTokenHash(eq(oldTokenHash));

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).save(eq(existingToken));
            inOrder.verify(repository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when token does not exist")
        void rotateAndIssueRefreshToken_shouldThrowException_whenTokenNotFound() {

            // Given
            String rawToken = "nonexistent-token";
            String tokenHash = "nonexistent-hash";

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);

            when(repository.findByTokenHash(eq(tokenHash))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.rotateAndIssueRefreshToken(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token not found");

            // Verify
            verify(hasher).hash(eq(rawToken));
            verify(repository).findByTokenHash(eq(tokenHash));
            verifyNoInteractions(generator);
        }

        @Test
        @DisplayName("Should throw RefreshTokenReuseException and revoke all user sessions when token is already revoked")
        void rotateAndIssueRefreshToken_shouldThrowReuseException_whenTokenIsRevoked() {

            // Given
            String activeTokenHash = "active-token-hash";
            String revokedRawToken = "revoked-raw-token";
            String revokedTokenHash = "revoked-token-hash";

            User user = createSavedUser();
            RefreshToken activeToken = createActiveToken(user, activeTokenHash);
            RefreshToken revokedToken = createRevokedToken(user, revokedTokenHash);

            when(hasher.hash(eq(revokedRawToken))).thenReturn(revokedTokenHash);

            when(repository.findByTokenHash(eq(revokedTokenHash)))
                    .thenReturn(Optional.of(revokedToken));

            when(repository.findByUserIdAndRevokedFalse(eq(user.getId())))
                    .thenReturn(List.of(activeToken));

            // When & Then
            assertThatThrownBy(
                    () -> refreshTokenService.rotateAndIssueRefreshToken(revokedRawToken))
                            .isInstanceOf(RefreshTokenReuseException.class)
                            .hasMessageContaining("Refresh token reuse detected");

            assertThat(activeToken.isRevoked()).isTrue();

            // Verify
            verify(repository).findByUserIdAndRevokedFalse(eq(user.getId()));
            verify(repository).saveAll(List.of(activeToken));
            verifyNoInteractions(generator);
        }

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when token is expired")
        void rotateAndIssueRefreshToken_shouldThrowException_whenTokenIsExpired() {

            // Given
            String rawToken = "expired-raw-token";
            String tokenHash = "expired-token-hash";

            User user = createSavedUser();
            RefreshToken expiredToken = createExpiredToken(user, tokenHash);

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);
            when(repository.findByTokenHash(eq(tokenHash))).thenReturn(Optional.of(expiredToken));

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.rotateAndIssueRefreshToken(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token has expired");

            // Verify
            verify(repository).findByTokenHash(eq(tokenHash));
            verify(repository, never()).save(any());
            verifyNoInteractions(generator);
        }
    }

    @Nested
    @DisplayName("Get User From Refresh Token Tests")
    class GetUserFromRefreshTokenTests {

        @Test
        @DisplayName("Should return the User associated with a valid token")
        void getUserFromRefreshToken_shouldReturnUser_whenTokenIsValid() {

            // Given
            String rawToken = "valid-raw-token";
            String tokenHash = "valid-token-hash";

            User user = createSavedUser();
            RefreshToken token = createActiveToken(user, tokenHash);

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);
            when(repository.findByTokenHash(eq(tokenHash))).thenReturn(Optional.of(token));

            // When
            User result = refreshTokenService.getUserFromRefreshToken(rawToken);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(user.getId());
            assertThat(result.getEmail()).isEqualTo(user.getEmail());

            // Verify
            verify(hasher).hash(eq(rawToken));
            verify(repository).findByTokenHash(eq(tokenHash));
        }

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when token does not exist")
        void getUserFromRefreshToken_shouldThrowException_whenTokenNotFound() {

            // Given
            String rawToken = "nonexistent-token";
            String tokenHash = "nonexistent-hash";

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);
            when(repository.findByTokenHash(eq(tokenHash))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.getUserFromRefreshToken(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token not found");

            // Verify
            verify(hasher).hash(eq(rawToken));
            verify(repository).findByTokenHash(eq(tokenHash));
        }

        @Test
        @DisplayName("Should throw RefreshTokenReuseException and revoke all user sessions when token is already revoked")
        void getUserFromRefreshToken_shouldThrowReuseException_whenTokenIsRevoked() {

            // Given
            String activeTokenHash = "active-token-hash";
            String revokedRawToken = "revoked-raw-token";
            String revokedTokenHash = "revoked-token-hash";

            User user = createSavedUser();
            RefreshToken activeToken = createActiveToken(user, activeTokenHash);
            RefreshToken revokedToken = createRevokedToken(user, revokedTokenHash);

            when(hasher.hash(eq(revokedRawToken))).thenReturn(revokedTokenHash);

            when(repository.findByTokenHash(eq(revokedTokenHash)))
                    .thenReturn(Optional.of(revokedToken));

            when(repository.findByUserIdAndRevokedFalse(eq(user.getId())))
                    .thenReturn(List.of(activeToken));

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.getUserFromRefreshToken(revokedRawToken))
                    .isInstanceOf(RefreshTokenReuseException.class)
                    .hasMessageContaining("Refresh token reuse detected");

            assertThat(activeToken.isRevoked()).isTrue();

            // Verify
            verify(repository).findByUserIdAndRevokedFalse(eq(user.getId()));
            verify(repository).saveAll(List.of(activeToken));
            verifyNoInteractions(generator);
        }

        @Test
        @DisplayName("Should throw InvalidRefreshTokenException when token is expired")
        void getUserFromRefreshToken_shouldThrowException_whenTokenIsExpired() {

            // Given
            String rawToken = "expired-raw-token";
            String tokenHash = "expired-token-hash";

            User user = createSavedUser();
            RefreshToken expiredToken = createExpiredToken(user, tokenHash);

            when(hasher.hash(eq(rawToken))).thenReturn(tokenHash);
            when(repository.findByTokenHash(eq(tokenHash))).thenReturn(Optional.of(expiredToken));

            // When & Then
            assertThatThrownBy(() -> refreshTokenService.getUserFromRefreshToken(rawToken))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Refresh token has expired");
                
            // Verify
            verify(repository).findByTokenHash(eq(tokenHash));
            verifyNoMoreInteractions(repository);
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

    private RefreshToken createRevokedToken(User user, String tokenHash) {
        Instant expiresAt = Instant.now().plusSeconds(604800);
        RefreshToken token = new RefreshToken(tokenHash, user, expiresAt);
        token.revoke();
        return token;
    }

    private RefreshToken createExpiredToken(User user, String tokenHash) {
        Instant expiresAt = Instant.now().minusSeconds(3600);
        return new RefreshToken(tokenHash, user, expiresAt);
    }
}
