package backend.secureauthapi.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a user's account status (active/inactive).
 * This operation is restricted to administrators only.
 */
public record UpdateUserStatusRequest(

    @NotNull(message = "Active status must be provided")
    Boolean active

) {}