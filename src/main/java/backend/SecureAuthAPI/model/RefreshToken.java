package backend.secureauthapi.model;

import java.time.Instant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
/**
 * Refresh Token used for access token renewal without reauthentication.
 * Stored hashed in the database, supports expiration and revocation.
 */
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Token expiration timestamp - tokens cannot be used after this time
    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Soft delete flag - true when token is manually revoked or rotated
    @Column(nullable = false)
    private boolean revoked = false;

    // Token creation timestamp - set automatically on first persist
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Rotation timestamp - set when token is used and replaced with a new one
    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Public constructor for JPA
    public RefreshToken(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    // This ensures the timestamp is set exactly when the entity is saved to the DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    // Invalidate the token manually
    public void revoke() {
        this.revoked = true;
    }

    // Invalidate the token when it is used
    public void rotate() {
        this.revoked = true;
        this.rotatedAt = Instant.now();
    }

    // Setters for optional metadata
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}