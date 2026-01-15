package backend.secureauthapi.security.util;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

    private static final int DEFAULT_TOKEN_BYTE_LENGTH = 64;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generates a cryptographically secure refresh token value.
     * The generated token is URL-safe and contains 512 bits of entropy,
     * making it suitable for use in authentication systems.
     *
     * Important: This method returns the raw token value.
     * It must be hashed before being persisted to the database.
     *
     * @return a secure, random refresh token value
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[DEFAULT_TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}