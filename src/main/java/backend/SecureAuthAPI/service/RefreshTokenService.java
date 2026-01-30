package backend.secureauthapi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.exception.token.InvalidRefreshTokenException;
import backend.secureauthapi.exception.token.RefreshTokenReuseException;
import backend.secureauthapi.model.RefreshToken;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.RefreshTokenRepository;
import backend.secureauthapi.security.util.RefreshTokenGenerator;
import backend.secureauthapi.security.util.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing refresh token lifecycle including issuance, rotation,
 * and revocation.
 * Implements token rotation strategy and reuse detection for enhanced security.
 * All tokens are stored hashed in the database and use soft delete for audit
 * trail.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${security.jwt.refresh-expiration-ms}") private Long refreshTokenDurationMs;

    private final RefreshTokenRepository repository;
    private final RefreshTokenGenerator generator;
    private final RefreshTokenHasher hasher;
    private final Clock clock;

    /**
     * Issues a new refresh token for the given user and persists its hashed value.
     * The raw token value is returned to the caller and must never be stored in
     * plain text.
     */
    @Transactional
    public String issueRefreshToken(
            User user,
            String deviceInfo,
            String ipAddress) {

        String rawToken = generator.generateRefreshToken();
        String tokenHash = hasher.hash(rawToken);
        Instant expiresAt = Instant.now(clock).plus(refreshTokenDurationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = new RefreshToken(tokenHash, user, expiresAt);

        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);

        repository.save(refreshToken);

        /*
         * Return the raw token to the client; this is the only time the plain-text
         * value is exposed. The server only persists the hash for security.
         */
        return rawToken;
    }

    /**
     * Finds an active (non-revoked, non-expired) refresh token by its raw value.
     * Used internally for token validation.
     */
    private Optional<RefreshToken> findByToken(String token) {
        String tokenHash = hasher.hash(token);
        return repository.findByTokenHashAndRevokedFalse(tokenHash);
    }

    /**
     * Revokes a single refresh token (soft delete).
     * Used for single-device logout.
     */
    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hasher.hash(rawToken);

        repository.findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    repository.save(refreshToken);
                });
    }

    /**
     * Revokes all active refresh tokens for a user (soft delete).
     * Used for logout all devices or password change scenarios.
     */
    @Transactional
    public void revokeAllTokensByUser(Long userId) {
        List<RefreshToken> activeTokens = repository.findByUserIdAndRevokedFalse(userId);
        activeTokens.forEach(RefreshToken::revoke);
        repository.saveAll(activeTokens);
    }

    /**
     * Rotates an existing refresh token by revoking it and issuing a new one.
     * Implements token reuse detection - if a revoked token is used, all user
     * sessions are revoked.
     */
    @Transactional
    public String rotateAndIssueRefreshToken(String rawToken) {
        String tokenHash = hasher.hash(rawToken);

        RefreshToken existingToken = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        validateToken(existingToken);

        existingToken.rotate();
        repository.save(existingToken);

        return issueRefreshToken(
                existingToken.getUser(),
                existingToken.getDeviceInfo(),
                existingToken.getIpAddress());
    }

    /**
     * Retrieves the user associated with a refresh token.
     * Validates the token before returning the user.
     */
    @Transactional(readOnly = true)
    public User getUserFromRefreshToken(String rawToken) {
        String tokenHash = hasher.hash(rawToken);

        RefreshToken token = repository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        validateToken(token);

        return token.getUser();
    }

    /**
     * Validates a refresh token for rotation.
     * Detects token reuse attacks and revokes all user sessions if detected.
     */
    private void validateToken(RefreshToken token) {
        if (token.isRevoked()) {
            revokeAllTokensByUser(token.getUser().getId());
            throw new RefreshTokenReuseException();
        }

        if (token.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }
    }

    /**
     * Checks if a refresh token is valid for rotation.
     * Returns true if the token is active (non-revoked, non-expired).
     */
    public boolean isTokenValid(String rawToken) {
        return findByToken(rawToken)
                .map(RefreshToken::isActive)
                .orElse(false);
    }
}
