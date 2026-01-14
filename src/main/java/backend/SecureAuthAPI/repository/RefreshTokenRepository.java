package backend.secureauthapi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import backend.secureauthapi.model.RefreshToken;
import jakarta.transaction.Transactional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by its value.
     * This is used to validate the token when it's used to generate a new access
     * token.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Delete all the refresh tokens associated to a user.
     * Useful for cases of:
     * 1. Logout: Delete the current session.
     * 2. Password change: Delete ALL sessions (global logout).
     * Return an int indicating how many records were deleted.
     */
    @Modifying // This modifies the database, it's not a query
    @Transactional
    int deleteByUserId(Long userId);
}