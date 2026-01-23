package backend.secureauthapi.dto;

import java.time.Instant;
import backend.secureauthapi.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserResponse(

    @NotNull
    Long id,

    @NotBlank
    String name,

    @Email
    String email,

    @NotNull
    Role role,

    boolean isActive,

    @NotNull
    Instant createdAt

) {}