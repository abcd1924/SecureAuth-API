package backend.secureauthapi.dto.request;

import backend.secureauthapi.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description = "Request to update a user's role. This operation is restricted to administrators only. Role changes take effect immediately but do not invalidate existing tokens. Users will see their new permissions after their next login or token refresh."
)
public record UpdateUserRoleRequest(

    @Schema(
        description = "User role in the system",
        example = "ADMIN",
        allowableValues = {"USER", "ADMIN", "AUDITOR", "SUPPORT"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Role must be provided")
    Role role

) {}