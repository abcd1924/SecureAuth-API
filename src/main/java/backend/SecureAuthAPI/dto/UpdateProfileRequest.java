package backend.secureauthapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for updating user profile information.
 * Currently only supports updating the user's name.
 * Email changes are not allowed to maintain account integrity and avoid
 * conflicts with existing authentication tokens.
 */
public record UpdateProfileRequest(

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[\\p{L} .'-]+$",
        message = "Name contains invalid characters"
    )
    String name

) {}