package backend.secureauthapi.exception.auth;

/**
 * Exception thrown when a refresh token is invalid, expired, or not found.
 * This is a runtime exception that should be caught by the global exception
 * handler.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    /**
     * Constructs a new InvalidRefreshTokenException with the default detail
     * message.
     * Intended to be used when the specific cause is not relevant to the caller
     * and a generic "invalid refresh token" error is sufficient.
     */
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }

    /**
     * Constructs a new InvalidRefreshTokenException with the specified detail
     * message.
     *
     * @param message the detail message explaining why the token is invalid
     */
    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidRefreshTokenException with the specified detail
     * message and cause.
     *
     * @param message the detail message explaining why the token is invalid
     * @param cause   the cause of the exception
     */
    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}