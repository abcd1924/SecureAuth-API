package backend.SecureAuthAPI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import backend.secureauthapi.dto.request.UpdateProfileRequest;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.user.UserNotFoundException;
import backend.secureauthapi.mapper.UserMapper;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import backend.secureauthapi.service.RefreshTokenService;
import backend.secureauthapi.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("john@example.com", null);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /*
     * User Methods
     */

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return UserResponse of authenticated user")
        void getCurrentUser_shouldReturnUserResponse_whenUserIsAuthenticated() {

            // Given
            User user = createSavedUser();
            UserResponse expectedResponse = createUserResponse(user);

            when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(eq(user))).thenReturn(expectedResponse);

            // When
            UserResponse result = userService.getCurrentUser();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedResponse.id());
            assertThat(result.email()).isEqualTo(expectedResponse.email());
            assertThat(result.role()).isEqualTo(Role.USER);

            // Verify
            verify(userRepository).findByEmail(eq(user.getEmail()));
            verify(userMapper).toUserResponse(eq(user));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when authenticated user does not exist")
        void getCurrentUser_shouldThrowException_whenUserNotFoundInDatabase() {

            // Given
            when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

            // Verify
            verify(userRepository).findByEmail(eq("john@example.com"));
            verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update user name and return updated UserResponse")
        void updateProfile_shouldReturnUpdateUserResponse_whenUserExists() {

            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe");
            User user = createSavedUser();

            User updatedUser = createSavedUser();
            updatedUser.setName("Jane Doe");

            UserResponse expectedResponse = new UserResponse(1L, "Jane Doe", "john@example.com",
                    Role.USER, true, user.getCreatedAt());

            when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.of(user));
            when(userRepository.save(eq(user))).thenReturn(updatedUser);
            when(userMapper.toUserResponse(eq(updatedUser))).thenReturn(expectedResponse);

            // When
            UserResponse result = userService.updateProfile(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(request.name());
            assertThat(result.email()).isEqualTo("john@example.com");

            // Verify
            verify(userRepository).findByEmail(eq("john@example.com"));
            verify(userRepository).save(eq(user));
            verify(userMapper).toUserResponse(eq(updatedUser));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when authenticated user does not exist")
        void updateProfile_shouldThrowException_whenUserNotFound() {

            // Given
            UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe");

            when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

            // Verify
            verify(userRepository).findByEmail("john@example.com");
            verify(userRepository, never()).save(any());
            verifyNoInteractions(userMapper);
        }
    }

    private User createSavedUser() {
        User user = new User("John Doe", "john@example.com", "encodedPassword", Role.USER);
        user.setId(1L);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private UserResponse createUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
