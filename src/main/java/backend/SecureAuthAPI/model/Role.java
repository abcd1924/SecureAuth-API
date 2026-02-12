package backend.secureauthapi.model;

import org.springframework.security.core.GrantedAuthority;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the roles available in the application context.
 * Implements GrantedAuthority to facilitate direct integration with Spring
 * Security context.
 */
@Schema(
    description = """
        User role within the system:
        - ADMIN: Full administrative access, including system configuration and user management
        - USER: Standard user with permissions limited to managing their own account
        - AUDITOR: Read-only access for auditing and compliance
        - SUPPORT: Customer support with restricted administrative privileges
        """
)
public enum Role implements GrantedAuthority {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
    AUDITOR("ROLE_AUDITOR"),
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