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
import java.util.List;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import backend.secureauthapi.dto.request.ChangePasswordRequest;
import backend.secureauthapi.dto.request.UpdateProfileRequest;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.user.PasswordChangeException;
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

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password, encode it and revoke all user sesions")
        void changePassword_shouldEncodePasswordAndRevokeSessions_whenCurrentPasswordIsCorrect() {

            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!", "NewPass456!");
            User user = createSavedUser();

            String newEncodedPassword = "encodedPassword";

            when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));

            when(passwordEncoder.matches(eq(request.currentPassword()), eq(user.getPasswordHash())))
                    .thenReturn(true);

            when(passwordEncoder.encode(eq(request.newPassword()))).thenReturn(newEncodedPassword);

            // When
            MessageResponse result = userService.changePassword(request);

            // Then
            assertThat(result).isNotNull();

            assertThat(result.message())
                    .isEqualTo("Password changed successfully. Please login again.");

            assertThat(user.getPasswordHash()).isEqualTo(newEncodedPassword);

            assertThat(result.timestamp()).isNotNull().isBeforeOrEqualTo(Instant.now());

            // Verify
            verify(passwordEncoder).matches(eq(request.currentPassword()),
                    eq(user.getPasswordHash()));

            verify(passwordEncoder).encode(eq(request.newPassword()));

            verify(userRepository).save(eq(user));

            verify(refreshTokenService).revokeAllTokensByUser(eq(user.getId()));
        }

        @Test
        @DisplayName("Should throw PasswordChangeException when current password is incorrect")
        void changePassword_shouldThrowException_whenCurrentPasswordIsIncorrect() {

            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("WrongPass!", "NewPass456!");
            User user = createSavedUser();

            when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));

            when(passwordEncoder.matches(eq(request.currentPassword()), eq(user.getPasswordHash())))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(PasswordChangeException.class)
                    .hasMessageContaining("Current password is incorrect");

            // Verify
            verify(passwordEncoder).matches(eq(request.currentPassword()),
                    eq(user.getPasswordHash()));

            verify(passwordEncoder, never()).encode(any());

            verify(userRepository, never()).save(any());

            verifyNoInteractions(refreshTokenService);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when authenticated user does not exist")
        void changePassword_shouldThrowException_whenUserNotFound() {

            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!", "NewPass456!");

            when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

            // Verify
            verifyNoInteractions(passwordEncoder, refreshTokenService);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Deactivate Account Tests")
    class DeactivateAccountTests {

        @Test
        @DisplayName("Should deactivate account and revoke all user sessions")
        void deactivateAccount_shouldDeactivateAndRevokeAllSessions_whenUserExists() {

            // Given
            User user = createSavedUser();

            when(userRepository.findByEmail(eq(user.getEmail()))).thenReturn(Optional.of(user));

            // When
            MessageResponse result = userService.deactivateAccount();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.message()).isEqualTo("Account deactivated successfully");
            assertThat(user.isActive()).isFalse();
            assertThat(result.timestamp()).isNotNull().isBeforeOrEqualTo(Instant.now());

            // Verify
            verify(userRepository).findByEmail(eq(user.getEmail()));
            verify(userRepository).save(eq(user));
            verify(refreshTokenService).revokeAllTokensByUser(user.getId());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when authenticated user does not exist")
        void deactivateAccount_shouldThrowException_whenUserNotFound() {

            // Given
            when(userRepository.findByEmail(eq("john@example.com"))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.deactivateAccount())
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

            // Verify
            verify(userRepository).findByEmail(eq("john@example.com"));
            verify(userRepository, never()).save(any());
            verifyNoInteractions(refreshTokenService);
        }
    }

    /*
     * Admin Methods
     */

    @Nested
    @DisplayName("Get all Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return page with mapped users when users exists")
        void getAllUsers_shouldReturnPageWithMappedUsers_whenUsersExists() {

            // Given
            Pageable pageable = PageRequest.of(0, 5);

            User user1 = createSavedUser();
            User user2 = createSavedUser(); user2.setId(2L);
            List<User> users = List.of(user1, user2);
            Page<User> userPage = new PageImpl<>(users, pageable, users.size());

            UserResponse response1 = createUserResponse(user1);
            UserResponse response2 = createUserResponse(user2);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toUserResponse(user1)).thenReturn(response1);
            when(userMapper.toUserResponse(user2)).thenReturn(response2);

            // When
            Page<UserResponse> result = userService.getAllUsers(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).containsExactly(response1, response2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getNumber()).isZero();

            // Verify
            verify(userRepository).findAll(eq(pageable));
            verify(userMapper).toUserResponse(user1);
            verify(userMapper).toUserResponse(user2);
        }

        @Test
        @DisplayName("Should return empty page when there are no users")
        void getAllUsers_shouldReturnEmptyPage_whenNoUsersFound() {

            // Given
            Pageable pageable = PageRequest.of(0, 5);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(userRepository.findAll(eq(pageable))).thenReturn(emptyPage);

            // When
            Page<UserResponse> result = userService.getAllUsers(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();

            // Verify
            verify(userRepository).findAll(eq(pageable));
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
