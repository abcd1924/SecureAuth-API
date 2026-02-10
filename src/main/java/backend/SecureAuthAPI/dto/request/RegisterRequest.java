package backend.secureauthapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new user")
public record RegisterRequest(

    @Schema(
        description = "User name",
        example = "John Doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Name is required") 
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") 
    String name,

    @Schema(
        description = "User email",
        example = "john.doe@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required") 
    @Email(message = "Email must be a valid email address") 
    @Size(max = 100, message = "Email must not exceed 100 characters") 
    String email,

    @Schema(
        description = "Strong password (min 8 chars, must include: uppercase, lowercase, digit and special char)",
        example = "SecurePass123!",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required") 
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters") 
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character") 
    String password

) {}