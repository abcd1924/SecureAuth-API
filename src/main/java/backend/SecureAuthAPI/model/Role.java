package backend.secureauthapi.model;

import org.springframework.security.core.GrantedAuthority;

/**
 * Defines the roles available in the application context.
 * Implements GrantedAuthority to facilitate direct integration with Spring
 * Security context.
 */
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