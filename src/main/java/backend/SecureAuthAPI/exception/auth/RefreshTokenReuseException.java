package backend.secureauthapi.exception.auth;

/**
 * Exception thrown when a refresh token is reused.
 * This is a runtime exception that should be caught by the global exception
 * handler.
 */
public class RefreshTokenReuseException extends RuntimeException {

    /**
     * Constructs a new RefreshTokenReuseException with the default detail
     * message.
     * Intended to be used when the specific cause is not relevant to the caller
     * and a generic "refresh token reuse detected" error is sufficient.
     */
    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }

    /**
     * Constructs a new RefreshTokenReuseException with the specified detail
     * message.
     * 
     * @param message the detail message explaining why the token is invalid
     */
    public RefreshTokenReuseException(String message) {
        super(message);
    }

    /**
     * Constructs a new RefreshTokenReuseException with the specified detail
     * message and cause.
     * 
     * @param message the detail message explaining why the token is invalid
     * @param cause   the cause of the exception
     */
    public RefreshTokenReuseException(String message, Throwable cause) {
        super(message, cause);
    }
}