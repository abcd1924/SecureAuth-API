package backend.secureauthapi.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotNull;

public record LoginResponse(

    @NotNull
    String accessToken,

    @NotNull
    String refreshToken,

    @NotNull
    TokenType tokenType,

    @NotNull
    Instant expiresAt,

    @NotNull
    UserResponse user

) {
    /**
     * Convenience constructor that defaults tokenType to "Bearer".
     */
    public LoginResponse(String accessToken, String refreshToken, Instant expiresAt, UserResponse user) {
        this(accessToken, refreshToken, TokenType.BEARER, expiresAt, user);
    }

    public enum TokenType {
        BEARER
    }
}