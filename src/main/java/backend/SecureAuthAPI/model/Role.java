package backend.secureauthapi.model;

import org.springframework.security.core.GrantedAuthority;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the roles available in the application context.
 * Implements GrantedAuthority to facilitate direct integration with Spring
 * Security context.
 */
@Schema(description = "User role in the system")
public enum Role implements GrantedAuthority {
    @Schema(description = "Full administrative access, including system configuration and user management")
    ADMIN("ROLE_ADMIN"),

    @Schema(description = "Standard user with permissions limited to managing their own account and personal data")
    USER("ROLE_USER"),

    @Schema(description = "Read-only access for auditing and compliance purposes")
    AUDITOR("ROLE_AUDITOR"),

    @Schema(description = "Customer support role with restricted administrative privileges")
    SUPPORT("ROLE_SUPPORT");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    /**
     * Returns the role name formatted for Spring Security (e.g., "ROLE_ADMIN").
     */
    @Override
    public String getAuthority() {
        return authority;
    }
}