package backend.secureauthapi.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Token refresh response with new access and refresh tokens after token rotation")
public record RefreshTokenResponse(

    @Schema(
        description = "New JWT access token for API authentication",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    String accessToken,

    @Schema(
        description = "New refresh token (old token is invalidated due to rotation)",
        example = "b2c3d4e5-f6g7-8901-bcde-fg2345678901",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    String refreshToken,

    @Schema(
        description = "Type of token (always 'Bearer' for JWT)",
        example = "BEARER",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    LoginResponse.TokenType tokenType,

    @Schema(
        description = "New access token expiration timestamp (ISO 8601 format)",
        example = "2026-02-09T23:30:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
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