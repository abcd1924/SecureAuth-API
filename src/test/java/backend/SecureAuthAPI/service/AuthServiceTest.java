package backend.SecureAuthAPI.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.dto.request.RegisterRequest;
import backend.secureauthapi.dto.response.LoginResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.auth.InvalidCredentialsException;
import backend.secureauthapi.exception.user.UserAlreadyExistsException;
import backend.secureauthapi.mapper.UserMapper;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import backend.secureauthapi.security.UserDetailsImpl;
import backend.secureauthapi.security.jwt.JwtUtils;
import backend.secureauthapi.service.AuthService;
import backend.secureauthapi.service.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthService authService;

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
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return LoginResponse when credentials are valid")
        void login_shouldReturnLoginResponse_whenCredentialsAreValid() {

            // Given
            LoginRequest request = createValidLoginRequest();
            String deviceInfo = "Chrome/Windows";
            String ipAddress = "192.168.1.1";

            User user = createSavedUser("encodedPassword");

            UserDetailsImpl userDetails = UserDetailsImpl.build(user);

            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());

            String accessToken = "jwt.access.token";
            String refreshToken = "refresh-token-uuid";
            UserResponse userResponse = createUserResponse(user);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(jwtUtils.generateAccessToken(eq(userDetails))).thenReturn(accessToken);

            when(userRepository.findByEmail(eq(request.email()))).thenReturn(Optional.of(user));

            when(refreshTokenService.issueRefreshToken(eq(user), eq(deviceInfo), eq(ipAddress)))
                    .thenReturn(refreshToken);

            when(userMapper.toUserResponse(eq(user))).thenReturn(userResponse);

            // When
            LoginResponse result = authService.login(request, deviceInfo, ipAddress);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(accessToken);
            assertThat(result.refreshToken()).isEqualTo(refreshToken);
            assertThat(result.tokenType()).isEqualTo(LoginResponse.TokenType.BEARER);
            assertThat(result.expiresAt()).isNotNull().isAfter(Instant.now());
            assertThat(result.user()).isEqualTo(userResponse);

            // Verify
            verify(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils).generateAccessToken(eq(userDetails));
            verify(userRepository).findByEmail(eq(request.email()));
            verify(refreshTokenService).issueRefreshToken(eq(user), eq(deviceInfo), eq(ipAddress));
            verify(userMapper).toUserResponse(eq(user));
        }

        @Test
        @DisplayName("Should throw exception when authentication fails")
        void login_shouldThrowException_whenAuthenticationFails() {

            // Given
            LoginRequest request = createValidLoginRequest();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad crendentials"));

            // When & Then
            assertThatThrownBy(() -> authService.login(request, "Chrome", "127.0.0.1"))
                    .isInstanceOf(BadCredentialsException.class);

            // Verify
            verify(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));
            verifyNoInteractions(jwtUtils, userRepository, refreshTokenService, userMapper);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user is not found in database")
        void login_shouldThrowException_whenUserNotFoundInDatabase() {

            // Given
            LoginRequest request = createValidLoginRequest();

            User user = createSavedUser("encodedPassword");
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);

            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

            when(jwtUtils.generateAccessToken(eq(userDetails))).thenReturn("some.token");

            when(userRepository.findByEmail(eq(request.email()))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.login(request, "Chrome", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

            // Verify
            verify(userRepository).findByEmail(eq(request.email()));
            verifyNoInteractions(refreshTokenService, userMapper);
        }

        private LoginRequest createValidLoginRequest() {
            return new LoginRequest("john@example.com", "SecurePass123!");
        }

        private UserResponse createUserResponse(User user) {
            return new UserResponse(1L, "John", "john@example.com", Role.USER, true, Instant.now());
        }
    }

    private User createSavedUser(String encodedPassword) {
        User user = new User("John Doe", "john@example.com", encodedPassword, Role.USER);
        user.setId(1L);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
