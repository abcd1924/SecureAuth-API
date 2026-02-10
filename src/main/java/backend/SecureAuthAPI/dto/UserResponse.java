package backend.secureauthapi.dto;

import java.time.Instant;
import backend.secureauthapi.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "User information response (password is never included)")
public record UserResponse(

    @Schema(
        description = "Unique user identifier",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Long id,

    @Schema(
        description = "User name",
        example = "John Doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String name,

    @Schema(
        description = "User email",
        example = "john.doe@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Email
    String email,

    @Schema(
        description = "User role in the system",
        example = "USER",
        allowableValues = {"USER", "ADMIN", "AUDITOR", "SUPPORT"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Role role,

    @Schema(
        description = "Whether the user account is active",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isActive,

    @Schema(
        description = "Account creation timestamp (ISO 8601 format)",
        example = "2026-02-09T23:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Instant createdAt

) {}