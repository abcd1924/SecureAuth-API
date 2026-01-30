package backend.secureauthapi.mapper;

import org.springframework.stereotype.Component;
import backend.secureauthapi.dto.RegisterRequest;
import backend.secureauthapi.dto.UserResponse;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;

@Component
public class UserMapper {

    /**
     * Converts a User entity to a UserResponse DTO.
     * Excludes sensitive information like passwordHash.
     */
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    /**
     * Converts a RegisterRequest DTO to a User entity.
     * The password must be hashed before calling this method.
     * The role is set to USER by default.
     */
    public User toEntity(RegisterRequest request, String passwordHash) {
        return toEntity(request, passwordHash, Role.USER);
    }

    /**
     * Converts a RegisterRequest DTO to a User entity with a specific role.
     * The password must be hashed before calling this method.
     * This overload allows creating users with different roles (e.g., ADMIN).
     */
    public User toEntity(RegisterRequest request, String passwordHash, Role role) {
        if (request == null) {
            return null;
        }

        return new User(
                request.name(),
                request.email(),
                passwordHash,
                role
        );
    }
}