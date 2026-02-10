package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to refresh an access token using a valid refresh token")
public record RefreshTokenRequest(

    @Schema(
        description = "Refresh token obtained from login or previous refresh",
        example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Refresh token is required")
    String refreshToken

) {}