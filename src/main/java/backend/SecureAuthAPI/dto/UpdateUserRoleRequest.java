package backend.secureauthapi.dto;

import backend.secureauthapi.model.Role;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a user's role.
 * This operation is restricted to administrators only.
 * Role changes take effect immediately but do not invalidate existing tokens.
 * Users will see their new permissions after their next login or token refresh.
 */
public record UpdateUserRoleRequest(

    @NotNull(message = "Role must be provided")
    Role role

) {}