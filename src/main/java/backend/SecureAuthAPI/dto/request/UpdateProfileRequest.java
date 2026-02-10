package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to update user profile information. Currently only supports updating the user's name.")
public record UpdateProfileRequest(

    @Schema(
        description = "User name",
        example = "María O'Connor-Smith",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[\\p{L} .'-]+$",
        message = "Name contains invalid characters"
    )
    String name

) {}