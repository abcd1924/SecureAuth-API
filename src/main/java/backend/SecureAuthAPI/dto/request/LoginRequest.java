package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to authenticate a user")
public record LoginRequest(

    @Schema(
        description = "User email",
        example = "user@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    String email,

    @Schema(
        description = "User password",
        example = "SecurePass123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    String password

) {}