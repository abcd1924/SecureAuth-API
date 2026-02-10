package backend.secureauthapi.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Authentication response containing JWT tokens and user information")
public record LoginResponse(

    @Schema(
        description = "JWT access token for API authentication",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    String accessToken,

    @Schema(
        description = "Refresh token for obtaining new access tokens",
        example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
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
    TokenType tokenType,

    @Schema(
        description = "Access token expiration timestamp (ISO 8601 format)",
        example = "2026-02-09T23:15:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Instant expiresAt,

    @Schema(
        description = "Authenticated user information",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    UserResponse user

) {
    /**
     * Convenience constructor that defaults tokenType to "Bearer".
     */
    public LoginResponse(String accessToken, String refreshToken, Instant expiresAt, UserResponse user) {
        this(accessToken, refreshToken, TokenType.BEARER, expiresAt, user);
    }

    @Schema(description = "Token type enumeration")
    public enum TokenType {
        @Schema(description = "Bearer token type for JWT authentication")
        BEARER
    }
}