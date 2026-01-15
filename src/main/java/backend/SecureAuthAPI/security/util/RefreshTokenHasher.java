package backend.secureauthapi.security.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * This service implements a critical security practice: storing only the hash
 * of tokens rather than the raw token values. This ensures that even if the
 * database is ompromised, attackers cannot directly use the stored tokens to
 * impersonate users.
 * 
 * Security rationale:
 * Raw tokens are never stored in the database
 * SHA-256 provides one-way hashing (irreversible)
 * Token validation is done by hashing the incoming token and comparing hashes
 * Database breach does not expose usable authentication credentials
 */
@Component
public class RefreshTokenHasher {

    /**
     * Hashes a refresh token using SHA-256 algorithm.
     * The token is converted to bytes using UTF-8 encoding, then hashed using
     * SHA-256, and finally encoded as a hexadecimal string. The resulting hash is
     * always 64 characters long (256 bits = 32 bytes = 64 hex characters).
     * 
     * @param token the raw refresh token to hash (must not be null)
     * @return a 64-character hexadecimal string representing the SHA-256 hash
     * @throws IllegalStateException if SHA-256 algorithm is not available (should
     *                               never happen in modern JVMs)
     */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}