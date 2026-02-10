package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
    description = "Request to change user password. It requires the current password for verification to prevent unauthorized password changes even if the session is compromised"
)
public record ChangePasswordRequest(

    @Schema(
        description = "Current password",
        example = "OldSecure123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @Schema(
        description = "New password (must be different from current)",
        example = "NewSecure456!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 50, message = "New password must be between 8 and 50 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
    String newPassword

) {
    // IMPORTANT: Contains sensitive data. Do not log.
}