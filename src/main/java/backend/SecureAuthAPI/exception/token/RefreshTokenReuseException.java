package backend.secureauthapi.exception.token;

/**
 * Exception thrown when a refresh token is reused.
 * This is considered a security incident and typically triggers
 * revocation of all active sessions for the user.
 */
public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse detected");
    }

    public RefreshTokenReuseException(String message) {
        super(message);
    }

    public RefreshTokenReuseException(String message, Throwable cause) {
        super(message, cause);
    }
}