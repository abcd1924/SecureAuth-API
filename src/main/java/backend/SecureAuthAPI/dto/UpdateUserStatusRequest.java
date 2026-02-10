package backend.secureauthapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    description = "Request to update a user's account status (active/inactive). This operation is restricted to administrators only."
)
public record UpdateUserStatusRequest(

    @Schema(
        description = "User account status",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Active status must be provided")
    Boolean active

) {}