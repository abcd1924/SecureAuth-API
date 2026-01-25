package backend.secureauthapi.dto;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MessageResponse(

    @NotBlank
    String message,

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