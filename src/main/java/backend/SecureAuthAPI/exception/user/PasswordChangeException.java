package backend.secureauthapi.exception.user;

/**
 * Exception thrown when a user provides an incorrect current password
 * during password change operations.
 */
public class PasswordChangeException extends RuntimeException {

    public PasswordChangeException() {
        super("Password change failed");
    }

    public PasswordChangeException(String message) {
        super(message);
    }

    public PasswordChangeException(String message, Throwable cause) {
        super(message, cause);
    }
}