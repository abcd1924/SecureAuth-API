package backend.secureauthapi.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Generic message response for simple operations")
public record MessageResponse(

    @Schema(
        description = "Response message describing the operation result",
        example = "Logout successful",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String message,

    @Schema(
        description = "Timestamp when the response was generated (ISO 8601 format)",
        example = "2026-02-09T23:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull
    Instant timestamp

) {
    /**
     * Convenience constructor that automatically sets the current timestamp.
     */
    public MessageResponse(String message) {
        this(message, Instant.now());
    }
}