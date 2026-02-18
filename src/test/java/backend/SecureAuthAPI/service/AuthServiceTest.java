package backend.SecureAuthAPI.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import backend.secureauthapi.dto.request.RegisterRequest;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.user.UserAlreadyExistsException;
import backend.secureauthapi.mapper.UserMapper;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import backend.secureauthapi.security.jwt.JwtUtils;
import backend.secureauthapi.service.AuthService;
import backend.secureauthapi.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock 
    private UserRepository userRepository;
    @Mock 
    private PasswordEncoder passwordEncoder;
    @Mock 
    private AuthenticationManager authenticationManager;
    @Mock 
    private JwtUtils jwtUtils;
    @Mock 
    private RefreshTokenService refreshTokenService;
    @Mock 
    private UserMapper userMapper;
    @Mock 
    private UserDetailsService userDetailsService;

    @InjectMocks 
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully when email is unique")
        void register_shouldReturnUserResponse_whenEmailIsUnique() {

            // Given
            RegisterRequest request = createValidRegisterRequest();

            String encodedPassword = "encodedPassword123";
            User savedUser = createSavedUser(encodedPassword);

            UserResponse expectedResponse = new UserResponse(
                    1L,
                    "John Doe",
                    "john@example.com",
                    Role.USER,
                    true,
                    savedUser.getCreatedAt());

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
            when(userMapper.toEntity(eq(request), eq(encodedPassword))).thenReturn(savedUser);
            when(userRepository.save(eq(savedUser))).thenReturn(savedUser);
            when(userMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

            // When
            UserResponse result = authService.register(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedResponse.id());
            assertThat(result.name()).isEqualTo(expectedResponse.name());
            assertThat(result.email()).isEqualTo(expectedResponse.email());
            assertThat(result.role()).isEqualTo(Role.USER);
            assertThat(result.isActive()).isTrue();
            assertThat(result.createdAt()).isEqualTo(expectedResponse.createdAt());

            // Verify
            verify(userRepository).existsByEmail(request.email());
            verify(passwordEncoder).encode(request.password());
            verify(userRepository).save(eq(savedUser));
            verify(userMapper).toEntity(eq(request), eq(encodedPassword));
            verify(userMapper).toUserResponse(savedUser);
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_shouldThrowException_whenEmailAlreadyExists() {

            // Given
            RegisterRequest request = createValidRegisterRequest();

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("User already exists");

            // Verify
            verify(userRepository).existsByEmail(request.email());
            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(any());
            verify(userMapper, never()).toEntity(any(), any());
            verify(userMapper, never()).toUserResponse(any());
        }

        @Test
        @DisplayName("Should encode password before saving user")
        void register_shouldEncodePassword_beforeSavingUser() {

            // Given
            RegisterRequest request = createValidRegisterRequest();

            String encodedPassword = "encodedPassword123";
            User savedUser = createSavedUser(encodedPassword);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
            when(userMapper.toEntity(eq(request), eq(encodedPassword))).thenReturn(savedUser);
            when(userRepository.save(eq(savedUser))).thenReturn(savedUser);
            when(userMapper.toUserResponse(savedUser)).thenReturn(
                    new UserResponse(1L, "John Doe", "john@example.com", Role.USER, true,
                            savedUser.getCreatedAt()));

            // When
            authService.register(request);

            // Then
            verify(passwordEncoder).encode(request.password());
            verify(userMapper).toEntity(eq(request), eq(encodedPassword));
            verify(userMapper, never()).toEntity(eq(request), eq(request.password()));
        }

        private RegisterRequest createValidRegisterRequest() {
            return new RegisterRequest(
                    "John Doe",
                    "john@example.com",
                    "SecurePass123!");
        }

        private User createSavedUser(String encodedPassword) {
            User user = new User("John Doe", "john@example.com", encodedPassword, Role.USER);
            user.setId(1L);
            user.setCreatedAt(Instant.now());

            return user;
        }
    }
}