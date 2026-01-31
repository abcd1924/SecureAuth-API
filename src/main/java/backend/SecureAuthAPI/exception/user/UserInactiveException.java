package backend.secureauthapi.exception.user;

public class UserInactiveException extends RuntimeException {

    public UserInactiveException() {
        super("User account is inactive");
    }

    public UserInactiveException(String message) {
        super(message);
    }

    public UserInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}