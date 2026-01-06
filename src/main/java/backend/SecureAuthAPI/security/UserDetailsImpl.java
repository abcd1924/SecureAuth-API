package backend.secureauthapi.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;
import backend.secureauthapi.model.User;

public class UserDetailsImpl implements UserDetails {
    private final Long id;
    private final String email;

    @JsonIgnore
    private final String passwordHash;

    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String email, String passwordHash,
            boolean isActive, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isActive = isActive;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                List.of(user.getRole()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    /**
     * Immutable user identifier for auditing, authorization and token usage
     */
    public Long getId() {
        return id;
    }
}