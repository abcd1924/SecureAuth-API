package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to invalidate a refresh token and logout the user")
public record LogoutRequest(

    @Schema(
        description = "Refresh token to be invalidated",
        example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token is required")
    String refreshToken

) {
    // IMPORTANT: Contains sensitive data. Do not log.
}