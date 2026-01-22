package backend.secureauthapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Name is required") 
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") 
    String name,

    @NotBlank(message = "Email is required") 
    @Email(message = "Email must be a valid email address") 
    @Size(max = 100, message = "Email must not exceed 100 characters") 
    String email,

    @NotBlank(message = "Password is required") 
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters") 
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character") 
    String password

) {}