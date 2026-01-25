package backend.secureauthapi.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;

/**
 * Response DTO for token refresh operations.
 * Returns a new access token and refresh token pair after successful token rotation.
 * The refresh token is rotated for security (one-time use policy).
 */
public record RefreshTokenResponse(

    @NotNull
    String accessToken,

    @NotNull
    String refreshToken,

    @NotNull
    LoginResponse.TokenType tokenType,

    @NotNull
    Instant expiresAt

) {
    /**
     * Convenience constructor that defaults tokenType to "Bearer".
     */
    public RefreshTokenResponse(String accessToken, String refreshToken, Instant expiresAt) {
        this(accessToken, refreshToken, LoginResponse.TokenType.BEARER, expiresAt);
    }
}