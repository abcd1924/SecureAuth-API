package backend.secureauthapi.exception;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Standard error response structure for all API exceptions.
 * Provides consistent error information to clients across the entire API.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code (e.g., 400, 401, 404, 500).
     */
    private final int status;

    /**
     * Human-readable error message describing what went wrong or an generic error
     * message.
     */
    private final String message;

    /**
     * Timestamp when the error occurred (ISO-8601 format).
     */
    private final Instant timestamp;

    /**
     * Request path that caused the error (e.g., "/api/auth/login").
     */
    private final String path;

    /**
     * Optional error code for client-side error handling.
     * Examples: "INVALID_REFRESH_TOKEN", "USER_NOT_FOUND", "INVALID_CREDENTIALS"
     */
    private final String errorCode;
}