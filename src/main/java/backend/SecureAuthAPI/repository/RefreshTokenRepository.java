package backend.secureauthapi.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import backend.secureauthapi.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by its value.
     * This is used to validate the token when it's used to generate a new access
     * token.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Find all active (non-revoked) refresh tokens for a specific user.
     * Used for revoking all sessions (e.g., logout all devices, password change).
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * Find a refresh token by its hash, regardless of revocation status.
     * Used for detecting token reuse attacks.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}