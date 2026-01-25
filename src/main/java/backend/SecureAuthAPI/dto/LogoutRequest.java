package backend.secureauthapi.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(

    @NotBlank(message = "Refresh token is required")
    String refreshToken

) {
    // IMPORTANT: Contains sensitive data. Do not log.
}